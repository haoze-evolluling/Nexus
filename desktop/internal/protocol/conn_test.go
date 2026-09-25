package protocol

import (
	"strings"
	"testing"
)

func TestConnRequestRoundTrip(t *testing.T) {
	b, err := EncodeConn(ConnControl{Kind: ConnRequest, DeviceID: "android-0123456789abcdef", Name: "Pixel 9 speaker"})
	if err != nil {
		t.Fatal(err)
	}
	c, err := DecodeConn(b)
	if err != nil || c.Kind != ConnRequest || c.DeviceID != "android-0123456789abcdef" || c.Name != "Pixel 9 speaker" {
		t.Fatalf("decoded %#v err=%v", c, err)
	}
}

func TestConnResponseRoundTrip(t *testing.T) {
	for _, allow := range []bool{true, false} {
		b, err := EncodeConn(ConnControl{Kind: ConnResponse, DeviceID: "pc-uuid-42", Allow: allow})
		if err != nil {
			t.Fatal(err)
		}
		c, err := DecodeConn(b)
		if err != nil || c.Kind != ConnResponse || c.DeviceID != "pc-uuid-42" || c.Allow != allow {
			t.Fatalf("allow=%v decoded %#v err=%v", allow, c, err)
		}
	}
}

func TestConnByeRoundTrip(t *testing.T) {
	b, err := EncodeConn(ConnControl{Kind: ConnBye, DeviceID: "pc-uuid-42"})
	if err != nil {
		t.Fatal(err)
	}
	c, err := DecodeConn(b)
	if err != nil || c.Kind != ConnBye || c.DeviceID != "pc-uuid-42" || c.Name != "" {
		t.Fatalf("decoded %#v err=%v", c, err)
	}
}

func TestEncodeConnRejectsOversizedDeviceID(t *testing.T) {
	if _, err := EncodeConn(ConnControl{Kind: ConnBye, DeviceID: strings.Repeat("x", MaxDeviceIDLen+1)}); err == nil {
		t.Fatal("oversized device id accepted")
	}
}

func TestEncodeConnTruncatesLongName(t *testing.T) {
	b, err := EncodeConn(ConnControl{Kind: ConnRequest, DeviceID: "id", Name: strings.Repeat("n", 200)})
	if err != nil {
		t.Fatal(err)
	}
	c, err := DecodeConn(b)
	if err != nil || len(c.Name) != MaxDeviceNameLen {
		t.Fatalf("name length=%d err=%v", len(c.Name), err)
	}
}

func TestDecodeConnRejectsForeignDatagrams(t *testing.T) {
	cases := [][]byte{
		nil,
		[]byte("SVCT"),
		append([]byte("SVCR\x03\x01\x00\x00"), []byte("id")...), // wrong version
		[]byte("SVCR\x03\x09\x00\x00id\x00name"),                // unknown kind
		[]byte("SVCR\x03\x01\x00\x00\x00name"),                  // empty device id
		[]byte("SVCR\x03\x02\x00\x00id"),                        // response without decision
		[]byte("SVCR\x03\x02\x00\x00id\x00\x07"),                // unknown decision
	}
	for _, b := range cases {
		if _, err := DecodeConn(b); err == nil {
			t.Fatalf("accepted invalid datagram %q", b)
		}
	}
}

func TestTimeSyncRoundTrip(t *testing.T) {
	req := EncodeTimeSync(TimeSync{Kind: TimeSyncRequest, T1: 111})
	parsed, err := DecodeTimeSync(req)
	if err != nil || parsed.Kind != TimeSyncRequest || parsed.T1 != 111 || parsed.T2 != 0 || parsed.T3 != 0 {
		t.Fatalf("request=%+v err=%v", parsed, err)
	}
	resp := EncodeTimeSync(TimeSync{Kind: TimeSyncResponse, T1: 111, T2: 222, T3: 333})
	parsed, err = DecodeTimeSync(resp)
	if err != nil || parsed.Kind != TimeSyncResponse || parsed.T1 != 111 || parsed.T2 != 222 || parsed.T3 != 333 {
		t.Fatalf("response=%+v err=%v", parsed, err)
	}
}

func TestDecodeTimeSyncRejectsForeign(t *testing.T) {
	for _, bad := range [][]byte{
		[]byte("SVTS"),
		append([]byte("SVTS\x03\x01\x00\x00"), make([]byte, 34)...),
		append([]byte("SVTS\x04\x09\x00\x00"), make([]byte, 32)...),
	} {
		if _, err := DecodeTimeSync(bad); err == nil {
			t.Fatalf("accepted %q", bad[:6])
		}
	}
}
