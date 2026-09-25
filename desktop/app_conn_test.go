package main

import (
	"net"
	"testing"
	"time"

	"steamvoice-desktop/internal/config"
	"steamvoice-desktop/internal/gateway"
	"steamvoice-desktop/internal/protocol"
)

// newTestApp returns an app with an in-memory trust store and a running
// control listener on an ephemeral port, ready to receive requests from the
// returned client socket.
func newTestApp(t *testing.T) (*App, *net.UDPConn) {
	t.Helper()
	a := NewAppWithStore(config.Memory())
	listener, err := gateway.Start(0, "desktop-self", a.onConnRequest, a.onConnBye)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(listener.Close)
	a.listener = listener
	client, err := net.DialUDP("udp", nil, listener.Addr())
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = client.Close() })
	return a, client
}

func sendConn(t *testing.T, conn *net.UDPConn, msg protocol.ConnControl) {
	t.Helper()
	b, err := protocol.EncodeConn(msg)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := conn.Write(b); err != nil {
		t.Fatal(err)
	}
}

func readConnResponse(t *testing.T, conn *net.UDPConn) protocol.ConnControl {
	t.Helper()
	_ = conn.SetReadDeadline(time.Now().Add(time.Second))
	buf := make([]byte, 128)
	n, err := conn.Read(buf)
	if err != nil {
		t.Fatal(err)
	}
	msg, err := protocol.DecodeConn(buf[:n])
	if err != nil || msg.Kind != protocol.ConnResponse {
		t.Fatalf("response=%+v err=%v", msg, err)
	}
	return msg
}

func TestTrustedDeviceIsAutoAccepted(t *testing.T) {
	a, client := newTestApp(t)
	if err := a.store.Authorize("android-1", "Pixel 9"); err != nil {
		t.Fatal(err)
	}
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	if msg := readConnResponse(t, client); !msg.Allow {
		t.Fatal("trusted device must be auto-accepted")
	}
	if len(a.pending) != 0 {
		t.Fatal("trusted device must not wait for approval")
	}
}

// waitForPending blocks until the request from deviceID is registered.
func waitForPending(t *testing.T, a *App, deviceID string) string {
	t.Helper()
	deadline := time.Now().Add(time.Second)
	for time.Now().Before(deadline) {
		a.mu.Lock()
		id, ok := a.pendingByDevice[deviceID]
		a.mu.Unlock()
		if ok {
			return id
		}
		time.Sleep(5 * time.Millisecond)
	}
	t.Fatal("request was never registered")
	return ""
}

func TestUnknownDeviceWaitsForApprovalAndDeduplicates(t *testing.T) {
	a, client := newTestApp(t)
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	waitForPending(t, a, "android-1")
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	deadline := time.Now().Add(time.Second)
	for time.Now().Before(deadline) {
		a.mu.Lock()
		pendingCount := len(a.pending)
		byDevice := len(a.pendingByDevice)
		a.mu.Unlock()
		if pendingCount == 1 && byDevice == 1 {
			return
		}
		time.Sleep(5 * time.Millisecond)
	}
	t.Fatal("duplicate requests must collapse into one pending entry")
}

func TestRespondConnectionDenyBlocksFutureAutoAccept(t *testing.T) {
	a, client := newTestApp(t)
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	requestID := waitForPending(t, a, "android-1")
	if err := a.RespondConnection(requestID, false, false); err != nil {
		t.Fatal(err)
	}
	if msg := readConnResponse(t, client); msg.Allow {
		t.Fatal("denied request must be answered with deny")
	}
	if a.store.IsAuthorized("android-1") {
		t.Fatal("denied request must not be remembered")
	}
}

func TestRespondConnectionRememberAuthorizesDevice(t *testing.T) {
	a, client := newTestApp(t)
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	requestID := waitForPending(t, a, "android-1")
	// Connect itself needs the Opus encoder, so only the authorization and
	// response effects are asserted here.
	if err := a.RespondConnection(requestID, true, true); err != nil {
		t.Fatal(err)
	}
	if msg := readConnResponse(t, client); !msg.Allow {
		t.Fatal("allowed request must be answered with allow")
	}
	if !a.store.IsAuthorized("android-1") {
		t.Fatal("remember must persist the authorization")
	}
}

func TestRespondConnectionUnknownRequestFails(t *testing.T) {
	a, _ := newTestApp(t)
	if err := a.RespondConnection("missing", true, false); err == nil {
		t.Fatal("unknown request id must fail")
	}
}

func TestRequestExpiryClearsPending(t *testing.T) {
	a, client := newTestApp(t)
	a.mu.Lock()
	a.requestTimeout = 20 * time.Millisecond
	a.mu.Unlock()
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	time.Sleep(80 * time.Millisecond)
	a.mu.Lock()
	pendingCount := len(a.pending)
	a.mu.Unlock()
	if pendingCount != 0 {
		t.Fatalf("expired request still pending: %d", pendingCount)
	}
	if err := a.RespondConnection("whatever", true, false); err == nil {
		t.Fatal("expired request must no longer be answerable")
	}
}

func TestByeDisconnectsSession(t *testing.T) {
	a, client := newTestApp(t)
	a.sessions["android-1"] = newTestSession(t, "android-1", "Pixel 9")
	sendConn(t, client, protocol.ConnControl{Kind: protocol.ConnBye, DeviceID: "android-1"})
	deadline := time.Now().Add(time.Second)
	for a.GetStatus().ConnectedCount != 0 && time.Now().Before(deadline) {
		time.Sleep(5 * time.Millisecond)
	}
	if a.GetStatus().ConnectedCount != 0 {
		t.Fatal("bye must disconnect the session")
	}
}
