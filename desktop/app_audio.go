package main

import (
	"context"
	"log"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
)

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
