package stream

import (
	"net"
	"testing"
	"time"

	"steamvoice-desktop/internal/protocol"
)

func TestSenderAddsConfiguredBitrate(t *testing.T) {
	listener, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4(127, 0, 0, 1)})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	sender, err := NewSender(listener.LocalAddr().String(), 96000)
	if err != nil {
		t.Fatal(err)
	}
	defer sender.Close()
	if err = sender.SendOpus(0, []byte{0xf8, 0xff}); err != nil {
		t.Fatal(err)
	}
	if err = listener.SetReadDeadline(time.Now().Add(time.Second)); err != nil {
		t.Fatal(err)
	}
	buf := make([]byte, 256)
	n, _, err := listener.ReadFromUDP(buf)
	if err != nil {
		t.Fatal(err)
	}
	h, payload, err := protocol.Decode(buf[:n])
	if err != nil {
		t.Fatal(err)
	}
	if h.Bitrate != 96000 || string(payload) != string([]byte{0xf8, 0xff}) {
		t.Fatalf("header=%+v payload=%v", h, payload)
	}
}

func TestSenderSendsPCMCodec(t *testing.T) {
	listener, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4(127, 0, 0, 1)})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	sender, err := NewSender(listener.LocalAddr().String())
	if err != nil {
		t.Fatal(err)
	}
	defer sender.Close()
	want := []byte{0, 1, 2, 3}
	if err = sender.SendOpus(0, want); err != nil {
		t.Fatal(err)
	}
	_ = listener.SetReadDeadline(time.Now().Add(time.Second))
	buf := make([]byte, 256)
	n, _, err := listener.ReadFromUDP(buf)
	if err != nil {
		t.Fatal(err)
	}
	h, payload, err := protocol.Decode(buf[:n])
	if err != nil {
		t.Fatal(err)
	}
	if h.Codec != protocol.CodecOpus || string(payload) != string(want) {
		t.Fatalf("header=%+v payload=%v", h, payload)
	}
}

func TestFeedbackIdleResetsOnFeedback(t *testing.T) {
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
	if err = sender.SendOpus(0, []byte{0xf8, 0xff}); err != nil {
		t.Fatal(err)
	}
	_ = listener.SetReadDeadline(time.Now().Add(time.Second))
	buf := make([]byte, 256)
	n, _, err := listener.ReadFromUDP(buf)
	if err != nil {
		t.Fatal(err)
	}
	h, _, err := protocol.Decode(buf[:n])
	if err != nil {
		t.Fatal(err)
	}
	time.Sleep(60 * time.Millisecond)
	if idle := sender.FeedbackIdle(); idle < 50*time.Millisecond {
		t.Fatalf("idle = %v, want it to grow while no feedback arrives", idle)
	}
	feedback := protocol.EncodeFeedback(protocol.ReceiverFeedback{Session: h.Session, HighestSeq: h.Sequence, Received: 1, Bitrate: 128000})
	if _, err = listener.WriteToUDP(feedback, sender.LocalAddr().(*net.UDPAddr)); err != nil {
		t.Fatal(err)
	}
	for deadline := time.Now().Add(time.Second); time.Now().Before(deadline); time.Sleep(10 * time.Millisecond) {
		if sender.FeedbackIdle() < 30*time.Millisecond {
			return
		}
	}
	t.Fatal("feedback did not reset FeedbackIdle")
}

func TestSenderKeeps128KbpsFixedDespiteFeedback(t *testing.T) {
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
	if err = sender.SendOpus(0, []byte{0xf8, 0xff}); err != nil {
		t.Fatal(err)
	}
	_ = listener.SetReadDeadline(time.Now().Add(time.Second))
	buf := make([]byte, 256)
	n, _, err := listener.ReadFromUDP(buf)
	if err != nil {
		t.Fatal(err)
	}
	h, _, err := protocol.Decode(buf[:n])
	if err != nil {
		t.Fatal(err)
	}
	feedback := protocol.EncodeFeedback(protocol.ReceiverFeedback{Session: h.Session, HighestSeq: h.Sequence, Received: 1, Lost: 10, Queue: 5, Bitrate: 128000})
	if _, err = listener.WriteToUDP(feedback, sender.LocalAddr().(*net.UDPAddr)); err != nil {
		t.Fatal(err)
	}
	time.Sleep(50 * time.Millisecond)
	if err = sender.SendOpus(0, []byte{0xf8, 0xff}); err != nil {
		t.Fatal(err)
	}
	n, _, err = listener.ReadFromUDP(buf)
	if err != nil {
		t.Fatal(err)
	}
	h, _, err = protocol.Decode(buf[:n])
	if err != nil {
		t.Fatal(err)
	}
	if h.Bitrate != 128000 {
		t.Fatalf("bitrate changed to %d", h.Bitrate)
	}
}
