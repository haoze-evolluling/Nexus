package stream

import (
	"net"
	"testing"
	"time"

	"steamvoice-desktop/internal/protocol"
)

// fakeReceiver answers connection requests on the sender's socket.
func fakeReceiver(t *testing.T, listener *net.UDPConn, allow bool, respondTo *net.UDPAddr, done chan<- struct{}) {
	t.Helper()
	buf := make([]byte, 256)
	_ = listener.SetReadDeadline(time.Now().Add(2 * time.Second))
	n, senderAddr, err := listener.ReadFromUDP(buf)
	if err != nil {
		close(done)
		return
	}
	msg, err := protocol.DecodeConn(buf[:n])
	if err != nil || msg.Kind != protocol.ConnRequest || msg.DeviceID != "desktop-self" || msg.Name != "MyPC" {
		close(done)
		return
	}
	response, _ := protocol.EncodeConn(protocol.ConnControl{Kind: protocol.ConnResponse, DeviceID: "receiver-1", Allow: allow})
	target := respondTo
	if target == nil {
		target = senderAddr
	}
	_, _ = listener.WriteToUDP(response, target)
	close(done)
}

func TestRequestConnectionAccepts(t *testing.T) {
	listener, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4(127, 0, 0, 1)})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	sender, err := NewSender(listener.LocalAddr().String(), 128000)
	if err != nil {
		t.Fatal(err)
	}
	defer sender.Close()
	done := make(chan struct{})
	go fakeReceiver(t, listener, true, nil, done)
	if !sender.RequestConnection("desktop-self", "MyPC", 2*time.Second) {
		t.Fatal("expected allow")
	}
	<-done
}

func TestRequestConnectionRejects(t *testing.T) {
	listener, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4(127, 0, 0, 1)})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	sender, err := NewSender(listener.LocalAddr().String(), 128000)
	if err != nil {
		t.Fatal(err)
	}
	defer sender.Close()
	done := make(chan struct{})
	go fakeReceiver(t, listener, false, nil, done)
	if sender.RequestConnection("desktop-self", "MyPC", 2*time.Second) {
		t.Fatal("expected deny")
	}
	<-done
}

func TestRequestConnectionTimesOut(t *testing.T) {
	listener, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4(127, 0, 0, 1)})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	// Bind the sender to the listener's port family but point it elsewhere so
	// nobody answers; keep the listener open so the OS does not send ICMP
	// port-unreachable (which the connected socket would surface as an error).
	sender, err := NewSender("127.0.0.1:9", 128000)
	if err != nil {
		t.Fatal(err)
	}
	defer sender.Close()
	start := time.Now()
	if sender.RequestConnection("desktop-self", "MyPC", 300*time.Millisecond) {
		t.Fatal("expected timeout deny")
	}
	if elapsed := time.Since(start); elapsed > 2*time.Second {
		t.Fatalf("timeout respected poorly: %v", elapsed)
	}
}
