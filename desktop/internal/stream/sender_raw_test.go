package stream

import (
	"net"
	"steamvoice-desktop/internal/protocol"
	"testing"
	"time"
)

// Replays the byte-level response shape a real Android receiver sends: the
// receiver's own device id (never the echoed request payload), NUL, decision.
func TestRequestConnectionAcceptsRawResponse(t *testing.T) {
	listener, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4(127, 0, 0, 1)})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		buf := make([]byte, 512)
		for {
			n, src, err := listener.ReadFromUDP(buf)
			if err != nil {
				return
			}
			if n >= 8 && string(buf[:4]) == "SVCR" && buf[5] == 1 {
				id := []byte("receiver-echo")
				out := make([]byte, 8+len(id)+2)
				copy(out, "SVCR")
				out[4] = protocol.Version
				out[5] = 2
				copy(out[8:], id)
				out[8+len(id)] = 0
				out[8+len(id)+1] = 1
				_, _ = listener.WriteToUDP(out, src)
			}
		}
	}()
	sender, err := NewSender(listener.LocalAddr().String(), 128000)
	if err != nil {
		t.Fatal(err)
	}
	defer sender.Close()
	if !sender.RequestConnection("desktop-123", "MyPC", 5*time.Second) {
		t.Fatal("raw response was not accepted")
	}
}
