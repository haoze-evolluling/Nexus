package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"net"
	"os"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
	"steamvoice-desktop/internal/capture"
	"steamvoice-desktop/internal/codec"
	"steamvoice-desktop/internal/config"
	"steamvoice-desktop/internal/discovery"
	"steamvoice-desktop/internal/gateway"
	"steamvoice-desktop/internal/ntp"
	"steamvoice-desktop/internal/protocol"
	"steamvoice-desktop/internal/stream"
)

// 用户可见消息以前端可翻译的稳定码（svmsg:<code>[:<detail>]）传递，
// 前端按界面语言查表翻译；未识别的码或裸文本按原样展示。
const svmsgPrefix = "svmsg:"

func svMsg(code string) string { return svmsgPrefix + code }

func svMsgf(code string, detail string) string {
	if detail == "" {
		return svMsg(code)
	}
	return svmsgPrefix + code + ":" + detail
}

func svErr(code string, detail string) error { return fmt.Errorf("%s", svMsgf(code, detail)) }

type Device struct {
	Name, Host       string
	Port             int
	ID               string
	Codec            string
	SampleRate       int
	Channels         int
	Bitrate          int
	FrameMs          int
	SupportedFrameMs []int
	UpdatedAtMs      int64
	SettingsDeviceID string
}

// DeviceStatus is the per-receiver streaming state pushed on stream:status.
type DeviceStatus struct {
	DeviceID  string
	Name      string
	Connected bool
	Message   string
	Bitrate   int
	FrameMs   int
	// Phase mirrors the receiver's calibration progress (0-3) for UI restore.
	Phase int
	// ChannelRoute controls which audio channels are sent to this receiver:
	// "stereo" (default), "left", or "right".
	ChannelRoute string
}

// CalibrationProgress reports one receiver's multi-device sync status as it
// happens, pushed on calibration:progress. Phase follows the receiver's
// reported sync state: 1 measuring clock offset, 2 aligned and ramping up,
// 3 synchronized playback running.
type CalibrationProgress struct {
	DeviceID string
	Name     string
	Phase    int
	OffsetMs int
	RttMs    int
}

// Status aggregates every active receiver for GetStatus.
type Status struct {
	ConnectedCount int
	Message        string
	Devices        []DeviceStatus
}

// ConnRequestInfo describes an inbound connection request awaiting the
// user's decision, pushed on conn:request.
type ConnRequestInfo struct {
	RequestID string
	DeviceID  string
	Name      string
	Host      string
}

// Identity exposes this desktop's stable device identity to the frontend.
type Identity struct {
	DeviceID string
	Name     string
}

// feedbackStaleAfter is how long without receiver feedback marks a session
// unresponsive; receivers report roughly every 200 ms while playing.
const feedbackStaleAfter = 3 * time.Second

// requestExpiry is how long an unanswered authorization prompt stays alive
// before the desktop gives up and cancels it; it matches the receiver-side
// prompt lifetime so a late approval still has a matching desktop request.
const requestExpiry = 36 * time.Second

// keepaliveIdle is how long without capture data before the sender pads the
// stream with encoded silence. WASAPI loopback delivers nothing while the
// system plays no sound, and receivers hang up after ten seconds of silence;
// keep-alive frames keep every session alive across quiet passages.
const keepaliveIdle = 40 * time.Millisecond

// feedbackDropAfter is how long without feedback a session is considered gone
// and is cleaned up automatically.
const feedbackDropAfter = 30 * time.Second

// streaming before giving up. It deliberately outlives the receiver's 35 s
// prompt expiry so a late approval still completes the handshake.
const authorizationTimeout = 36 * time.Second

type deviceSession struct {
	device  Device
	sender  *stream.Sender
	encoder *codec.OpusEncoder
	stale   bool
	status  DeviceStatus
	// channelRoute controls which channels of the stereo PCM are forwarded:
	// "stereo" (default), "left", or "right". Protected by App.mu.
	channelRoute string
	// calib tracks the receiver-reported sync phase plus the last pushed
	// clock readout, throttling calibration:progress events.
	calib        int
	calibOffset  int
	calibRtt     int
	calibEmitted time.Time
}

type pendingRequest struct {
	info  ConnRequestInfo
	peer  gateway.Peer
	timer *time.Timer
}

type App struct {
	ctx             context.Context
	mu              sync.Mutex
	discoverer      *discovery.Browser
	advertiser      *discovery.Advertiser
	sessions        map[string]*deviceSession
	capture         *capture.Loopback
	frameMs         int
	staleAfter      time.Duration
	dropAfter       time.Duration
	requestTimeout  time.Duration
	store           *config.File
	listener        *gateway.Listener
	pending         map[string]*pendingRequest
	pendingByDevice map[string]string
	connecting      map[string]bool
	localBitrate    int
	localFrameMs    int
	clockAnchor     time.Time
	lastPCM         time.Time
}

// streamClock maps "now" into the audio timestamp timebase: nanoseconds
// since the capture epoch shared by every active session. Reading the anchor
// under lock keeps restarts race-free.
func (a *App) streamClock() uint64 {
	a.mu.Lock()
	anchor := a.clockAnchor
	a.mu.Unlock()
	if anchor.IsZero() {
		return 0
	}
	return uint64(time.Since(anchor))
}

func NewApp() *App {
	var store *config.File
	if path, err := config.DefaultPath(); err == nil {
		if loaded, err := config.Load(path); err == nil {
			store = loaded
		} else {
			log.Printf("config load failed, using in-memory identity: %v", err)
		}
	}
	if store == nil {
		store = config.Memory()
	}
	return NewAppWithStore(store)
}

// NewAppWithStore wires an explicit identity/trust store; tests use it to
// avoid touching the real config file.
func NewAppWithStore(store *config.File) *App {
	return &App{sessions: map[string]*deviceSession{}, staleAfter: feedbackStaleAfter, dropAfter: feedbackDropAfter, requestTimeout: requestExpiry, pending: map[string]*pendingRequest{}, pendingByDevice: map[string]string{}, connecting: map[string]bool{}, localBitrate: 128000, localFrameMs: 10, store: store}
}
func (a *App) Startup(ctx context.Context) {
	a.ctx = ctx
	if a.store.Name() == "" {
		if host, err := os.Hostname(); err == nil && strings.TrimSpace(host) != "" {
			_ = a.store.SetName(strings.TrimSpace(host))
		}
	}
	listener, err := gateway.Start(protocol.DesktopControlPort, a.store.DeviceID, a.onConnRequest, a.onConnBye)
	if err != nil {
		// The app still works as a sender; only inbound connections break.
		log.Printf("control listener unavailable (another instance running?): %v", err)
	} else {
		a.listener = listener
	}
	if advertiser, err := discovery.Advertise("SteamVoice-"+a.pcName(), a.store.DeviceID, protocol.DesktopControlPort); err != nil {
		log.Printf("mDNS advertise failed: %v", err)
	} else {
		a.advertiser = advertiser
	}
	go a.monitorLoop(ctx)
	go a.keepaliveLoop(ctx)
}

func (a *App) pcName() string {
	if name := a.store.Name(); name != "" {
		return name
	}
	return "PC"
}

func (a *App) Shutdown(context.Context) {
	a.mu.Lock()
	sessions := a.sessions
	c := a.capture
	a.sessions = map[string]*deviceSession{}
	a.capture = nil
	a.frameMs = 0
	listener := a.listener
	advertiser := a.advertiser
	a.listener = nil
	a.advertiser = nil
	for _, entry := range a.pending {
		entry.timer.Stop()
	}
	a.pending = map[string]*pendingRequest{}
	a.pendingByDevice = map[string]string{}
	a.mu.Unlock()
	if advertiser != nil {
		advertiser.Close()
	}
	if listener != nil {
		listener.Close()
	}
	if c != nil {
		c.Close()
	}
	for _, s := range sessions {
		_ = s.sender.SendBye(a.store.DeviceID)
		_ = s.sender.Close()
	}
}
func (a *App) DiscoverDevices() ([]Device, error) {
	if a.discoverer != nil {
		a.discoverer.Close()
	}
	b := discovery.NewBrowser(
		func(d discovery.Device) {
			runtime.EventsEmit(a.ctx, "device:found", Device{Name: d.Name, Host: d.Host, Port: d.Port, ID: d.ID, Codec: d.Codec, SampleRate: d.SampleRate, Channels: d.Channels, Bitrate: d.Bitrate, FrameMs: d.FrameMs, SupportedFrameMs: d.SupportedFrameMs, UpdatedAtMs: d.UpdatedAtMs, SettingsDeviceID: d.SettingsDeviceID})
		},
		func(deviceID string) {
			runtime.EventsEmit(a.ctx, "device:lost", deviceID)
		},
	)
	a.discoverer = b
	return nil, b.Start(a.ctx)
}

// Connect starts streaming to device. Multiple receivers may be connected at
// the same time; every session encodes independently so per-receiver bitrate
// adaptation keeps working. Reconnecting an already-connected device replaces
// its session.
func (a *App) Connect(device Device) error {
	a.mu.Lock()
	if a.connecting == nil {
		a.connecting = map[string]bool{}
	}
	if a.connecting[device.ID] {
		a.mu.Unlock()
		return svErr("err_connecting", device.ID)
	}
	a.connecting[device.ID] = true
	a.mu.Unlock()
	defer func() {
		a.mu.Lock()
		delete(a.connecting, device.ID)
		a.mu.Unlock()
	}()
	bitrate := device.Bitrate
	if bitrate == 0 {
		bitrate = 128000
	}
	if bitrate != 64000 && bitrate != 96000 && bitrate != 128000 && bitrate != 192000 {
		return svErr("err_bitrate", strconv.Itoa(bitrate))
	}
	frameMs := device.FrameMs
	if frameMs == 0 {
		frameMs = 10
	}
	if frameMs != 10 && frameMs != 20 {
		return svErr("err_frame", strconv.Itoa(frameMs))
	}
	if len(device.SupportedFrameMs) > 0 {
		supported := false
		for _, value := range device.SupportedFrameMs {
			if value == frameMs {
				supported = true
				break
			}
		}
		if !supported {
			return svErr("err_frame_receiver", strconv.Itoa(frameMs))
		}
	}
	if !strings.EqualFold(device.Codec, "opus") {
		return svErr("err_codec", device.Codec)
	}
	a.mu.Lock()
	activeFrameMs := a.frameMs
	a.mu.Unlock()
	if activeFrameMs != 0 && activeFrameMs != frameMs {
		return svErr("err_frame_in_use", strconv.Itoa(activeFrameMs))
	}
	sender, err := stream.NewSender(fmt.Sprintf("%s:%d", device.Host, device.Port), bitrate, frameMs)
	if err != nil {
		return err
	}
	sender.SetClock(a.streamClock)
	// The receiver decides whether this desktop may stream; inbound-initiated
	// sessions re-confirm instantly because the receiver started them.
	if !sender.RequestConnection(a.store.DeviceID, a.pcName(), authorizationTimeout) {
		_ = sender.Close()
		return svErr("err_denied", "")
	}
	encoder, err := codec.NewOpusEncoder(bitrate, frameMs)
	if err != nil {
		_ = sender.Close()
		return svErr("err_opus_init", err.Error())
	}
	session := &deviceSession{device: device, sender: sender, encoder: encoder, channelRoute: "stereo", status: DeviceStatus{DeviceID: device.ID, Name: device.Name, Connected: true, Message: svMsg("streaming"), Bitrate: bitrate, FrameMs: frameMs, Phase: 0, ChannelRoute: "stereo"}}
	sender.SetFeedbackCallback(func(f protocol.ReceiverFeedback) {
		if f.SyncState == protocol.SyncUnknown {
			return
		}
		a.mu.Lock()
		current, stillActive := a.sessions[device.ID]
		if !stillActive || current != session {
			a.mu.Unlock()
			return
		}
		phase := int(f.SyncState)
		now := time.Now()
		phaseChanged := phase != session.calib
		statsChanged := phase >= protocol.SyncAligned && (int(f.OffsetMs) != session.calibOffset || int(f.RttMs) != session.calibRtt)
		emit := phaseChanged || (statsChanged && now.Sub(session.calibEmitted) >= 500*time.Millisecond)
		progress := CalibrationProgress{DeviceID: device.ID, Name: device.Name, Phase: phase, OffsetMs: int(f.OffsetMs), RttMs: int(f.RttMs)}
		if emit {
			session.calibEmitted = now
			session.calibOffset = int(f.OffsetMs)
			session.calibRtt = int(f.RttMs)
		}
		session.calib = phase
		session.status.Phase = phase
		ctx := a.ctx
		a.mu.Unlock()
		if emit && ctx != nil {
			runtime.EventsEmit(ctx, "calibration:progress", progress)
		}
	})
	sender.SetBitrateCallback(func(next int) {
		if err := encoder.SetBitrate(next); err != nil {
			log.Printf("opus bitrate update failed: %v", err)
		}
		a.mu.Lock()
		current, stillActive := a.sessions[device.ID]
		status := session.status
		ctx := a.ctx
		if stillActive && current == session {
			session.status.Bitrate = next
			status = session.status
		} else {
			stillActive = false
		}
		a.mu.Unlock()
		if stillActive && ctx != nil {
			runtime.EventsEmit(ctx, "stream:status", status)
		}
	})
	if device.UpdatedAtMs > 0 || device.SettingsDeviceID != "" {
		if err := sender.SendSettings(protocol.Settings{BitrateKbps: uint32(bitrate), FrameMs: uint16(frameMs), UpdatedAtMs: device.UpdatedAtMs, DeviceID: device.SettingsDeviceID}); err != nil {
			log.Printf("settings sync failed: %v", err)
		}
	}
	a.mu.Lock()
	if a.frameMs != 0 && a.frameMs != frameMs {
		a.mu.Unlock()
		_ = sender.Close()
		return svErr("err_frame_in_use", strconv.Itoa(a.frameMs))
	}
	if a.capture == nil {
		a.clockAnchor = time.Now()
		c, err := capture.Start(frameMs, a.onPCM)
		if err != nil {
			a.clockAnchor = time.Time{}
			a.mu.Unlock()
			_ = sender.Close()
			return svErr("err_capture", err.Error())
		}
		a.capture = c
		a.frameMs = frameMs
	}
	previous := a.sessions[device.ID]
	a.sessions[device.ID] = session
	a.mu.Unlock()
	if previous != nil {
		_ = previous.sender.Close()
	}
	a.emitStatus(session.status)
	return nil
}

// Disconnect stops streaming to the receiver identified by deviceID.
func (a *App) Disconnect(deviceID string) error {
	a.mu.Lock()
	session, ok := a.sessions[deviceID]
	if ok {
		delete(a.sessions, deviceID)
	}
	var c *capture.Loopback
	if ok && len(a.sessions) == 0 {
		c = a.capture
		a.capture = nil
		a.frameMs = 0
		a.clockAnchor = time.Time{}

	}
	a.mu.Unlock()
	if c != nil {
		c.Close()
	}
	if !ok {
		return nil
	}
	_ = session.sender.SendBye(a.store.DeviceID)
	_ = session.sender.Close()
	a.emitStatus(DeviceStatus{DeviceID: deviceID, Name: session.device.Name, Message: svMsg("disconnected")})
	return nil
}

func (a *App) GetStatus() Status {
	a.mu.Lock()
	defer a.mu.Unlock()
	status := Status{Devices: make([]DeviceStatus, 0, len(a.sessions))}
	for _, s := range a.sessions {
		status.Devices = append(status.Devices, s.status)
	}
	sort.Slice(status.Devices, func(i, j int) bool { return status.Devices[i].Name < status.Devices[j].Name })
	status.ConnectedCount = len(status.Devices)
	if status.ConnectedCount == 0 {
		status.Message = svMsg("idle")
	} else {
		status.Message = svMsgf("connected", strconv.Itoa(status.ConnectedCount))
	}
	return status
}

// GetIdentity exposes the stable desktop identity used for authorization.
func (a *App) GetIdentity() Identity {
	return Identity{DeviceID: a.store.DeviceID, Name: a.pcName()}
}

type NTPStatus struct {
	Server    string
	OffsetMs  int64
	Reachable bool
}

func (a *App) GetNTPSettings() NTPStatus { return NTPStatus{Server: a.store.NTPServerName()} }
func (a *App) SaveNTPServer(server string) error {
	server = strings.TrimSpace(server)
	if server == "" {
		server = ntp.DefaultServer
	}
	if len(server) > 253 || strings.ContainsAny(server, " \t\r\n") {
		return svErr("err_ntp_server", "")
	}
	return a.store.SetNTPServer(server)
}
func (a *App) TestNTPServer() NTPStatus {
	server := a.store.NTPServerName()
	offset, err := ntp.Query(server, time.Second)
	return NTPStatus{Server: server, OffsetMs: offset.Milliseconds(), Reachable: err == nil}
}

// ListAuthorizedDevices returns the remembered receivers that may connect
// without a confirmation prompt.
func (a *App) ListAuthorizedDevices() []config.AuthorizedDevice {
	return a.store.List()
}

// RemoveAuthorizedDevice drops a remembered receiver so its next connection
// asks for confirmation again.
func (a *App) RemoveAuthorizedDevice(deviceID string) error {
	return a.store.Remove(deviceID)
}

// SaveLocalSettings mirrors the frontend audio settings into the backend so
// inbound-initiated sessions encode with the same parameters the user picked.
func (a *App) SaveLocalSettings(bitrate int, frameMs int) {
	if bitrate != 64000 && bitrate != 96000 && bitrate != 128000 && bitrate != 192000 {
		return
	}
	if frameMs != 10 && frameMs != 20 {
		return
	}
	a.mu.Lock()
	a.localBitrate = bitrate
	a.localFrameMs = frameMs
	a.mu.Unlock()
}

// SetChannelRoute changes the audio channel routing for an active session.
// route must be "stereo", "left", or "right"; any other value is rejected.
// The change takes effect on the very next PCM frame — no reconnection needed.
func (a *App) SetChannelRoute(deviceID string, route string) error {
	if route != "stereo" && route != "left" && route != "right" {
		return svErr("err_channel_route", route)
	}
	a.mu.Lock()
	s, ok := a.sessions[deviceID]
	if ok {
		s.channelRoute = route
		s.status.ChannelRoute = route
	}
	var status DeviceStatus
	if ok {
		status = s.status
	}
	ctx := a.ctx
	a.mu.Unlock()
	if !ok {
		return svErr("err_not_connected", deviceID)
	}
	if ctx != nil {
		runtime.EventsEmit(ctx, "stream:status", status)
	}
	return nil
}

// RespondConnection answers a pending inbound request. remember stores the
// device so future requests are auto-accepted.
func (a *App) RespondConnection(requestID string, allow bool, remember bool) error {
	a.mu.Lock()
	entry, ok := a.pending[requestID]
	if !ok {
		a.mu.Unlock()
		return svErr("err_request_expired", "")
	}
	delete(a.pending, requestID)
	delete(a.pendingByDevice, entry.peer.DeviceID)
	entry.timer.Stop()
	listener := a.listener
	a.mu.Unlock()
	if listener == nil {
		return svErr("err_control", "")
	}
	if remember && allow {
		if err := a.store.Authorize(entry.peer.DeviceID, entry.peer.Name); err != nil {
			log.Printf("persisting authorization failed: %v", err)
		}
	}
	if err := listener.Respond(entry.peer, allow); err != nil {
		return svErr("err_respond", err.Error())
	}
	if allow {
		go func() {
			if err := a.Connect(a.deviceFromPeer(entry.peer)); err != nil {
				log.Printf("inbound connect to %s failed: %v", entry.peer.Name, err)
				a.emitStatus(DeviceStatus{DeviceID: entry.peer.DeviceID, Name: entry.peer.Name, Message: svMsgf("err_connect", err.Error())})
			}
		}()
	}
	return nil
}

// onConnRequest handles a receiver-initiated connection: trusted or already
// streaming devices are accepted instantly, everything else waits for the
// authorization modal.
func (a *App) onConnRequest(peer gateway.Peer) {
	a.mu.Lock()
	session, streaming := a.sessions[peer.DeviceID]
	trusted := a.store.IsAuthorized(peer.DeviceID)
	if streaming || trusted {
		// Request retransmissions must not churn the live session; only
		// rebuild it when the peer shows up from a different address.
		sameAddress := streaming && session.device.Host == peer.Addr.IP.String()
		a.mu.Unlock()
		if err := a.listener.Respond(peer, true); err != nil {
			log.Printf("responding to %s failed: %v", peer.DeviceID, err)
			return
		}
		if !sameAddress {
			go func() {
				if err := a.Connect(a.deviceFromPeer(peer)); err != nil {
					log.Printf("inbound connect to %s failed: %v", peer.DeviceID, err)
				}
			}()
		}
		return
	}
	if oldID, dup := a.pendingByDevice[peer.DeviceID]; dup {
		if old, ok := a.pending[oldID]; ok {
			old.timer.Stop()
			delete(a.pending, oldID)
			// Retransmissions from the phone replace the pending request;
			// tell the frontend to drop the superseded modal entry too.
			if a.ctx != nil {
				runtime.EventsEmit(a.ctx, "conn:cancelled", oldID)
			}
		}
	}
	requestID := newRequestID()
	entry := &pendingRequest{info: ConnRequestInfo{RequestID: requestID, DeviceID: peer.DeviceID, Name: peer.Name, Host: peer.Addr.IP.String()}, peer: peer}
	entry.timer = time.AfterFunc(a.requestTimeout, func() { a.expireRequest(requestID, entry) })
	a.pending[requestID] = entry
	a.pendingByDevice[peer.DeviceID] = requestID
	ctx := a.ctx
	a.mu.Unlock()
	if ctx != nil {
		runtime.EventsEmit(ctx, "conn:request", entry.info)
	}
}

// onConnBye drops the session when a receiver says goodbye.
func (a *App) onConnBye(deviceID string, nonce uint64, _ *net.UDPAddr) {
	a.mu.Lock()
	session := a.sessions[deviceID]
	a.mu.Unlock()
	if session == nil || session.sender.ConnNonce() != nonce {
		return
	}
	if err := a.Disconnect(deviceID); err != nil {
		log.Printf("disconnect after bye from %s failed: %v", deviceID, err)
	}
}

func (a *App) expireRequest(requestID string, entry *pendingRequest) {
	a.mu.Lock()
	current, ok := a.pending[requestID]
	if !ok || current != entry {
		a.mu.Unlock()
		return
	}
	delete(a.pending, requestID)
	delete(a.pendingByDevice, entry.peer.DeviceID)
	ctx := a.ctx
	a.mu.Unlock()
	if ctx != nil {
		runtime.EventsEmit(ctx, "conn:cancelled", entry.info.RequestID)
	}
}

func (a *App) deviceFromPeer(peer gateway.Peer) Device {
	name := strings.TrimSpace(peer.Name)
	if name == "" {
		name = "Android device"
	}
	a.mu.Lock()
	bitrate, frameMs := a.localBitrate, a.localFrameMs
	a.mu.Unlock()
	return Device{Name: name, Host: peer.Addr.IP.String(), Port: protocol.ReceiverAudioPort, ID: peer.DeviceID, Codec: "opus", SampleRate: protocol.SampleRate, Channels: protocol.Channels, Bitrate: bitrate, FrameMs: frameMs, SupportedFrameMs: []int{10, 20}}
}

func newRequestID() string {
	var raw [8]byte
	if _, err := rand.Read(raw[:]); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(raw[:])
}

// onPCM runs on the WASAPI capture thread: stamp the frame with its capture
// time, then encode once per session and send. Real-time (not frame-count
// derived) timestamps keep the audio timeline consistent with the time-sync
// clock even across silent gaps where no frames are produced.
func (a *App) onPCM(pcm []byte) {
	a.mu.Lock()
	a.lastPCM = time.Now()
	anchor := a.clockAnchor
	sessions := make([]*deviceSession, 0, len(a.sessions))
	for _, s := range a.sessions {
		sessions = append(sessions, s)
	}
	a.mu.Unlock()
	if anchor.IsZero() {
		return
	}
	tsNs := uint64(time.Since(anchor))
	a.sendFrame(sessions, tsNs, pcm)
}

// sendFrame encodes and delivers one PCM frame (real or synthetic silence) to
// every session, stamped with its stream-clock capture time. Each session may
// have an independent channel route ("stereo", "left", "right"); the PCM is
// filtered per-session before encoding so no shared buffers are mutated.
func (a *App) sendFrame(sessions []*deviceSession, tsNs uint64, pcm []byte) {
	for _, s := range sessions {
		frame := filterPCM(pcm, s.channelRoute)
		encoded, err := s.encoder.EncodePCM(frame)
		if err == nil {
			err = s.sender.SendOpus(tsNs, encoded)
		}
		if err != nil {
			log.Printf("audio UDP send to %s failed: %v", s.device.Name, err)
		}
	}
}

// filterPCM applies a channel route to a 16-bit stereo interleaved PCM frame.
// "left"  → duplicate left channel  into both channels (L,L)
// "right" → duplicate right channel into both channels (R,R)
// anything else (including "") → return pcm unchanged (stereo pass-through).
// When routing is needed a new slice is allocated so the caller's buffer is
// never modified; for stereo the original slice is returned directly.
func filterPCM(pcm []byte, route string) []byte {
	if route != "left" && route != "right" {
		return pcm
	}
	out := make([]byte, len(pcm))
	for i := 0; i+3 < len(pcm); i += 4 {
		// Each frame step: bytes i,i+1 = left sample; i+2,i+3 = right sample.
		if route == "left" {
			out[i], out[i+1] = pcm[i], pcm[i+1]
			out[i+2], out[i+3] = pcm[i], pcm[i+1]
		} else {
			out[i], out[i+1] = pcm[i+2], pcm[i+3]
			out[i+2], out[i+3] = pcm[i+2], pcm[i+3]
		}
	}
	return out
}


// keepaliveLoop bridges WASAPI loopback's silent idle periods: while sessions
// exist but the capture device produces no data, it feeds Opus-encoded silence
// at the active frame cadence so receivers never hit their audio-silence
// timeout and both ends keep reporting a live connection.
func (a *App) keepaliveLoop(ctx context.Context) {
	ticker := time.NewTicker(10 * time.Millisecond)
	defer ticker.Stop()
	var lastKeepalive time.Time
	silence := map[int][]byte{}
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
		a.mu.Lock()
		frameMs := a.frameMs
		anchor := a.clockAnchor
		idle := time.Since(a.lastPCM)
		sessions := make([]*deviceSession, 0, len(a.sessions))
		for _, s := range a.sessions {
			sessions = append(sessions, s)
		}
		a.mu.Unlock()
		if frameMs == 0 || anchor.IsZero() || len(sessions) == 0 {
			lastKeepalive = time.Time{}
			continue
		}
		frameDuration := time.Duration(frameMs) * time.Millisecond
		if idle <= keepaliveIdle || (!lastKeepalive.IsZero() && time.Since(lastKeepalive) < frameDuration) {
			continue
		}
		frame, ok := silence[frameMs]
		if !ok {
			frame = make([]byte, 480*frameMs/10*2*2)
			silence[frameMs] = frame
		}
		lastKeepalive = time.Now()
		a.sendFrame(sessions, uint64(time.Since(anchor)), frame)
	}
}

// monitorLoop flags sessions whose receiver stopped reporting feedback.
func (a *App) monitorLoop(ctx context.Context) {
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
		for _, status := range a.checkStaleSessions() {
			a.emitStatus(status)
		}
	}
}

// checkStaleSessions toggles the unresponsive flag of every session whose
// receiver feedback went silent or resumed, and drops sessions that stayed
// silent long enough to be considered gone, returning the changed statuses.
func (a *App) checkStaleSessions() []DeviceStatus {
	a.mu.Lock()
	var updated []DeviceStatus
	var dropped []*deviceSession
	for id, s := range a.sessions {
		idle := s.sender.FeedbackIdle()
		if idle > a.dropAfter {
			delete(a.sessions, id)
			dropped = append(dropped, s)
			continue
		}
		stale := idle > a.staleAfter
		if stale == s.stale {
			continue
		}
		s.stale = stale
		if stale {
			s.status.Message = svMsg("receiver_unresponsive")
		} else {
			s.status.Message = svMsg("streaming")
		}
		updated = append(updated, s.status)
	}
	var c *capture.Loopback
	if len(dropped) > 0 && len(a.sessions) == 0 {
		c = a.capture
		a.capture = nil
		a.frameMs = 0
		a.clockAnchor = time.Time{}
	}
	a.mu.Unlock()
	if c != nil {
		c.Close()
	}
	for _, s := range dropped {
		_ = s.sender.SendBye(a.store.DeviceID)
		_ = s.sender.Close()
		updated = append(updated, DeviceStatus{DeviceID: s.device.ID, Name: s.device.Name, Message: svMsg("receiver_dropped")})
	}
	return updated
}

func (a *App) emitStatus(status DeviceStatus) {
	a.mu.Lock()
	ctx := a.ctx
	a.mu.Unlock()
	if ctx != nil {
		runtime.EventsEmit(ctx, "stream:status", status)
	}
}
