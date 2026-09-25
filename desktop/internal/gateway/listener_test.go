package gateway

import (
	"net"
	"testing"
	"time"

	"steamvoice-desktop/internal/protocol"
)

func TestListenerDeliversRequestAndResponds(t *testing.T) {
	requests := make(chan Peer, 1)
	l, err := Start(0, "desktop-self", func(p Peer) { requests <- p }, func(string, *net.UDPAddr) {})
	if err != nil {
		t.Fatal(err)
	}
	defer l.Close()
	client, err := net.DialUDP("udp", nil, l.Addr())
	if err != nil {
		t.Fatal(err)
	}
	defer client.Close()
	req, err := protocol.EncodeConn(protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: "android-1", Name: "Pixel 9"})
	if err != nil {
		t.Fatal(err)
	}
	if _, err := client.Write(req); err != nil {
		t.Fatal(err)
	}
	select {
	case peer := <-requests:
		if peer.DeviceID != "android-1" || peer.Name != "Pixel 9" {
			t.Fatalf("peer=%+v", peer)
		}
		if err := l.Respond(peer, true); err != nil {
			t.Fatal(err)
		}
	case <-time.After(time.Second):
		t.Fatal("request not delivered")
	}
	_ = client.SetReadDeadline(time.Now().Add(time.Second))
	buf := make([]byte, 128)
	n, err := client.Read(buf)
	if err != nil {
		t.Fatal(err)
	}
	msg, err := protocol.DecodeConn(buf[:n])
	if err != nil || msg.Kind != protocol.ConnResponse || !msg.Allow || msg.DeviceID != "desktop-self" {
		t.Fatalf("response=%+v err=%v", msg, err)
	}
}

func TestListenerDeliversBye(t *testing.T) {
	byes := make(chan string, 1)
	l, err := Start(0, "desktop-self", func(Peer) {}, func(deviceID string, _ *net.UDPAddr) { byes <- deviceID })
	if err != nil {
		t.Fatal(err)
	}
	defer l.Close()
	client, err := net.DialUDP("udp", nil, l.Addr())
	if err != nil {
		t.Fatal(err)
	}
	defer client.Close()
	bye, err := protocol.EncodeConn(protocol.ConnControl{Kind: protocol.ConnBye, DeviceID: "android-1"})
	if err != nil {
		t.Fatal(err)
	}
	if _, err := client.Write(bye); err != nil {
		t.Fatal(err)
	}
	select {
	case id := <-byes:
		if id != "android-1" {
			t.Fatalf("bye id=%q", id)
		}
	case <-time.After(time.Second):
		t.Fatal("bye not delivered")
	}
}

func TestListenerIgnoresGarbage(t *testing.T) {
	l, err := Start(0, "desktop-self", func(Peer) { t.Fatal("garbage delivered as request") }, func(string, *net.UDPAddr) { t.Fatal("garbage delivered as bye") })
	if err != nil {
		t.Fatal(err)
	}
	defer l.Close()
	client, err := net.DialUDP("udp", nil, l.Addr())
	if err != nil {
		t.Fatal(err)
	}
	defer client.Close()
	for _, garbage := range [][]byte{[]byte("hello"), []byte("SVCS"), make([]byte, 64)} {
		if _, err := client.Write(garbage); err != nil {
			t.Fatal(err)
		}
	}
	time.Sleep(100 * time.Millisecond)
}
