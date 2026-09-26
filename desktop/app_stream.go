package main

import (
	"context"
	"fmt"
	"log"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
	"nexus-desktop/internal/capture"
	"nexus-desktop/internal/codec"
	"nexus-desktop/internal/protocol"
	"nexus-desktop/internal/stream"
)

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

// Connect starts streaming to device. Multiple receivers may be connected at
// the same time; every session encodes independently so per-receiver bitrate
// adaptation keeps working. Reconnecting an already-connected device replaces
// its session.
func (a *App) Connect(device Device) error {
	return a.connect(device, false, 0)
}

func (a *App) connect(device Device, inbound bool, nonce uint64) error {
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
	if inbound {
		if nonce != 0 {
			sender.SetConnNonce(nonce)
		}
	} else {
		// Outbound connection: ask the receiver for permission to stream.
		if !sender.RequestConnection(a.store.DeviceID, a.pcName(), authorizationTimeout) {
			_ = sender.Close()
			return svErr("err_denied", "")
		}
	}
	encoder, err := codec.NewOpusEncoder(bitrate, frameMs)
	if err != nil {
		_ = sender.Close()
		return svErr("err_opus_init", err.Error())
	}
	session := &deviceSession{
		device:       device,
		sender:       sender,
		encoder:      encoder,
		channelRoute: "stereo",
		status: DeviceStatus{
			DeviceID:     device.ID,
			Name:         device.Name,
			Connected:    true,
			Message:      svMsg("streaming"),
			Bitrate:      bitrate,
			FrameMs:      frameMs,
			Phase:        0,
			ChannelRoute: "stereo",
		},
	}
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
