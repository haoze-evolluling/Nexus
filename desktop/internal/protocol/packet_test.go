package protocol

import (
	"encoding/binary"
	"testing"
)

func TestRoundTrip(t *testing.T) {
	source := []byte{1, 2, 3, 4}
	b, e := Encode(Header{Session: 7, Sequence: 9, Bitrate: 128000}, source)
	if e != nil {
		t.Fatal(e)
	}
	h, got, e := Decode(b)
	if e != nil || h.Session != 7 || h.Sequence != 9 || h.Bitrate != 128000 || string(got) != string(source) {
		t.Fatalf("decoded %#v %v %v", h, got, e)
	}
}
func TestEncodedAudioFormatUsesBigEndianFields(t *testing.T) {
	b, err := Encode(Header{Bitrate: 96000}, nil)
	if err != nil {
		t.Fatal(err)
	}
	if got := binary.BigEndian.Uint32(b[6:10]); got != SampleRate {
		t.Fatalf("sample rate = %d", got)
	}
	if got := binary.BigEndian.Uint32(b[12:16]); got != 96000 {
		t.Fatalf("bitrate = %d", got)
	}
}

func TestRoundTripTwentyMillisecondFrame(t *testing.T) {
	b, err := Encode(Header{Session: 4, FrameMilliseconds: 20}, []byte{9, 8})
	if err != nil {
		t.Fatal(err)
	}
	h, got, err := Decode(b)
	if err != nil || h.FrameMilliseconds != 20 || string(got) != string([]byte{9, 8}) {
		t.Fatalf("decoded %#v %v %v", h, got, err)
	}
}

func TestTimestampRoundTrip(t *testing.T) {
	b, err := Encode(Header{TimestampNs: 1234567890123}, []byte{7})
	if err != nil {
		t.Fatal(err)
	}
	h, _, err := Decode(b)
	if err != nil || h.TimestampNs != 1234567890123 {
		t.Fatalf("timestamp=%d err=%v", h.TimestampNs, err)
	}
	if len(b) != HeaderSize+1 {
		t.Fatalf("header size drift: %d", len(b))
	}
}

func TestDecodeRejectsWrongVersion(t *testing.T) {
	b, err := Encode(Header{TimestampNs: 5}, []byte{1})
	if err != nil {
		t.Fatal(err)
	}
	b[4] = 3
	if _, _, err := Decode(b); err == nil {
		t.Fatal("stale protocol version accepted")
	}
}
