package main

import (
	"strings"
	"testing"
	"time"

	"steamvoice-desktop/internal/stream"
)

func newTestSession(t *testing.T, id string, name string) *deviceSession {
	t.Helper()
	sender, err := stream.NewSender("127.0.0.1:1", 128000, 10)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = sender.Close() })
	return &deviceSession{device: Device{ID: id, Name: name}, sender: sender, status: DeviceStatus{DeviceID: id, Name: name, Connected: true, Message: svMsg("streaming"), Bitrate: 128000, FrameMs: 10}}
}

func TestNewAppHasNoConnectedDevices(t *testing.T) {
	status := NewApp().GetStatus()
	if status.ConnectedCount != 0 {
		t.Fatalf("new app must have no connected devices, got %d", status.ConnectedCount)
	}
	if status.Message != svMsg("idle") {
		t.Fatalf("initial status message = %q, want %q", status.Message, svMsg("idle"))
	}
	if len(status.Devices) != 0 {
		t.Fatalf("new app device list = %v, want empty", status.Devices)
	}
}

func TestGetStatusAggregatesSessions(t *testing.T) {
	a := NewApp()
	a.sessions["phone-b"] = newTestSession(t, "phone-b", "Pixel")
	a.sessions["phone-a"] = newTestSession(t, "phone-a", "Galaxy")
	status := a.GetStatus()
	if status.ConnectedCount != 2 {
		t.Fatalf("connected count = %d, want 2", status.ConnectedCount)
	}
	if len(status.Devices) != 2 || status.Devices[0].DeviceID != "phone-a" || status.Devices[1].DeviceID != "phone-b" {
		t.Fatalf("devices = %+v, want sorted phone-a then phone-b", status.Devices)
	}
	if !strings.Contains(status.Message, "2") {
		t.Fatalf("aggregate message = %q, want it to mention 2 devices", status.Message)
	}
}

func TestConnectRejectsInvalidParameters(t *testing.T) {
	a := NewApp()
	cases := []struct {
		name   string
		device Device
		want   string
	}{
		{"bitrate", Device{Host: "127.0.0.1", Port: 1, ID: "x", Codec: "opus", Bitrate: 123000, FrameMs: 10}, "err_bitrate"},
		{"frame", Device{Host: "127.0.0.1", Port: 1, ID: "x", Codec: "opus", Bitrate: 128000, FrameMs: 30}, "err_frame"},
		{"codec", Device{Host: "127.0.0.1", Port: 1, ID: "x", Codec: "pcm", Bitrate: 128000, FrameMs: 10}, "err_codec"},
		{"unsupported by receiver", Device{Host: "127.0.0.1", Port: 1, ID: "x", Codec: "opus", Bitrate: 128000, FrameMs: 20, SupportedFrameMs: []int{10}}, "err_frame_receiver"},
	}
	for _, c := range cases {
		err := a.Connect(c.device)
		if err == nil || !strings.Contains(err.Error(), c.want) {
			t.Fatalf("%s: err = %v, want containing %q", c.name, err, c.want)
		}
	}
	if a.GetStatus().ConnectedCount != 0 {
		t.Fatal("rejected connects must not leave sessions behind")
	}
}

func TestConnectRejectsFrameChangeWhileStreaming(t *testing.T) {
	a := NewApp()
	a.frameMs = 10
	err := a.Connect(Device{Host: "127.0.0.1", Port: 1, ID: "x", Codec: "opus", Bitrate: 128000, FrameMs: 20, SupportedFrameMs: []int{10, 20}})
	if err == nil || err.Error() != svMsgf("err_frame_in_use", "10") {
		t.Fatalf("err = %v, want frame duration conflict svmsg:err_frame_in_use:10", err)
	}
}

func TestDisconnectRemovesOnlyTargetSession(t *testing.T) {
	a := NewApp()
	first := newTestSession(t, "phone-a", "Pixel")
	second := newTestSession(t, "phone-b", "Galaxy")
	a.sessions["phone-a"] = first
	a.sessions["phone-b"] = second
	if err := a.Disconnect("phone-a"); err != nil {
		t.Fatal(err)
	}
	status := a.GetStatus()
	if status.ConnectedCount != 1 || status.Devices[0].DeviceID != "phone-b" {
		t.Fatalf("after disconnect status = %+v, want only phone-b", status)
	}
	if err := a.Disconnect("unknown"); err != nil {
		t.Fatalf("disconnecting unknown device must be a no-op, got %v", err)
	}
	if a.GetStatus().ConnectedCount != 1 {
		t.Fatal("unknown disconnect must not touch other sessions")
	}
}

func TestCheckStaleSessionsFlagsSilentReceiver(t *testing.T) {
	a := NewApp()
	a.staleAfter = 10 * time.Millisecond
	session := newTestSession(t, "phone-a", "Pixel")
	a.sessions["phone-a"] = session
	time.Sleep(20 * time.Millisecond)
	updated := a.checkStaleSessions()
	if len(updated) != 1 || updated[0].Message != svMsg("receiver_unresponsive") {
		t.Fatalf("updated = %+v, want one receiver_unresponsive status", updated)
	}
	if session.status.Message != svMsg("receiver_unresponsive") {
		t.Fatalf("session message = %q, want %q", session.status.Message, svMsg("receiver_unresponsive"))
	}
	if again := a.checkStaleSessions(); len(again) != 0 {
		t.Fatalf("stable session re-reported: %+v", again)
	}
}

func TestCheckStaleSessionsDropsLongSilentReceiver(t *testing.T) {
	a := NewApp()
	a.staleAfter = 10 * time.Millisecond
	a.dropAfter = 40 * time.Millisecond
	session := newTestSession(t, "phone-a", "Pixel")
	a.sessions["phone-a"] = session
	time.Sleep(60 * time.Millisecond)
	updated := a.checkStaleSessions()
	if a.GetStatus().ConnectedCount != 0 {
		t.Fatal("long-silent receiver must be dropped")
	}
	if len(updated) != 1 || updated[0].Message != svMsg("receiver_dropped") {
		t.Fatalf("updated = %+v, want drop status", updated)
	}
}

func TestFilterPCM(t *testing.T) {
	// Sample PCM: 2 stereo frames (4 samples, 8 bytes).
	// Frame 0: Left = 0x0102, Right = 0x0304
	// Frame 1: Left = 0x0506, Right = 0x0708
	raw := []byte{0x02, 0x01, 0x04, 0x03, 0x06, 0x05, 0x08, 0x07}

	// Stereo pass-through returns the same slice
	stereo := filterPCM(raw, "stereo")
	if len(stereo) != len(raw) || &stereo[0] != &raw[0] {
		t.Errorf("filterPCM(stereo) must return input slice pointer directly")
	}

	// Left channel routing duplicates left to both channels
	left := filterPCM(raw, "left")
	wantLeft := []byte{0x02, 0x01, 0x02, 0x01, 0x06, 0x05, 0x06, 0x05}
	if string(left) != string(wantLeft) {
		t.Errorf("filterPCM(left) = %v, want %v", left, wantLeft)
	}
	if &left[0] == &raw[0] {
		t.Errorf("filterPCM(left) must return a newly allocated slice")
	}

	// Right channel routing duplicates right to both channels
	right := filterPCM(raw, "right")
	wantRight := []byte{0x04, 0x03, 0x04, 0x03, 0x08, 0x07, 0x08, 0x07}
	if string(right) != string(wantRight) {
		t.Errorf("filterPCM(right) = %v, want %v", right, wantRight)
	}
	if &right[0] == &raw[0] {
		t.Errorf("filterPCM(right) must return a newly allocated slice")
	}
}

func TestSetChannelRoute(t *testing.T) {
	a := NewApp()
	session := newTestSession(t, "phone-a", "Pixel")
	a.sessions["phone-a"] = session

	// Invalid route rejected
	if err := a.SetChannelRoute("phone-a", "invalid"); err == nil {
		t.Error("SetChannelRoute with invalid route must fail")
	}

	// Unknown device rejected
	if err := a.SetChannelRoute("unknown-device", "left"); err == nil {
		t.Error("SetChannelRoute with unknown device must fail")
	}

	// Valid switch to left
	if err := a.SetChannelRoute("phone-a", "left"); err != nil {
		t.Fatalf("SetChannelRoute to left failed: %v", err)
	}
	if session.channelRoute != "left" || session.status.ChannelRoute != "left" {
		t.Errorf("session channel route = %q, status = %q, want left", session.channelRoute, session.status.ChannelRoute)
	}

	// GetStatus reflects channel route
	status := a.GetStatus()
	if len(status.Devices) != 1 || status.Devices[0].ChannelRoute != "left" {
		t.Errorf("GetStatus ChannelRoute = %q, want left", status.Devices[0].ChannelRoute)
	}
}

