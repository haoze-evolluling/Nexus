package main

import (
	"fmt"
	"time"

	"nexus-desktop/internal/codec"
	"nexus-desktop/internal/gateway"
	"nexus-desktop/internal/stream"
)

// 用户可见消息以前端可翻译的稳定码（nxmsg:<code>[:<detail>]）传递，
// 前端按界面语言查表翻译；未识别的码或裸文本按原样展示。
const svmsgPrefix = "nxmsg:"

func svMsg(code string) string { return svmsgPrefix + code }

func svMsgf(code string, detail string) string {
	if detail == "" {
		return svMsg(code)
	}
	return svmsgPrefix + code + ":" + detail
}

func svErr(code string, detail string) error { return fmt.Errorf("%s", svMsgf(code, detail)) }

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

type NTPStatus struct {
	Server    string
	OffsetMs  int64
	Reachable bool
}

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
