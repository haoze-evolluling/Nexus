package main

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"net"
	"strings"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
	"nexus-desktop/internal/gateway"
	"nexus-desktop/internal/protocol"
	"nexus-desktop/internal/stream"
)

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
			if err := a.connect(a.deviceFromPeer(entry.peer), true, entry.peer.Nonce); err != nil {
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
		// Rebuild the session if:
		// 1) We are not currently streaming to this device, OR
		// 2) The peer reconnected from a different IP address, OR
		// 3) The peer sent a fresh Nonce (new connection / reconnect attempt), OR
		// 4) The existing session is stale (feedback timed out).
		// Only fast retransmissions with the exact same nonce on a live healthy session
		// are answered without churning the session.
		needsRebuild := !streaming ||
			session.device.Host != peer.Addr.IP.String() ||
			peer.Nonce != session.sender.ConnNonce() ||
			session.sender.FeedbackIdle() > feedbackStaleAfter
		var oldSender *stream.Sender
		if needsRebuild && streaming {
			delete(a.sessions, peer.DeviceID)
			oldSender = session.sender
		}
		a.mu.Unlock()
		if oldSender != nil {
			_ = oldSender.Close()
		}
		if err := a.listener.Respond(peer, true); err != nil {
			log.Printf("responding to %s failed: %v", peer.DeviceID, err)
			return
		}
		if needsRebuild {
			go func() {
				if err := a.connect(a.deviceFromPeer(peer), true, peer.Nonce); err != nil {
					log.Printf("inbound connect to %s failed: %v", peer.DeviceID, err)
					a.emitStatus(DeviceStatus{DeviceID: peer.DeviceID, Name: peer.Name, Message: svMsgf("err_connect", err.Error())})
				}
			}()
		}
		return
	}
	if oldID, dup := a.pendingByDevice[peer.DeviceID]; dup {
		if old, ok := a.pending[oldID]; ok {
			// Retransmissions from the phone while awaiting authorization:
			// keep the existing modal and requestID intact so the user can click it without flicker.
			// Just update the peer's latest network address and nonce in case they changed.
			old.peer = peer
			a.mu.Unlock()
			return
		}
	}
	requestID := newRequestID()
	entry := &pendingRequest{
		info:  ConnRequestInfo{RequestID: requestID, DeviceID: peer.DeviceID, Name: peer.Name, Host: peer.Addr.IP.String()},
		peer:  peer,
	}
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
	if session == nil || (nonce != 0 && session.sender.ConnNonce() != 0 && session.sender.ConnNonce() != nonce) {
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
	return Device{
		Name:             name,
		Host:             peer.Addr.IP.String(),
		Port:             protocol.ReceiverAudioPort,
		ID:               peer.DeviceID,
		Codec:            "opus",
		SampleRate:       protocol.SampleRate,
		Channels:         protocol.Channels,
		Bitrate:          bitrate,
		FrameMs:          frameMs,
		SupportedFrameMs: []int{10, 20},
	}
}

func newRequestID() string {
	var raw [8]byte
	if _, err := rand.Read(raw[:]); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(raw[:])
}
