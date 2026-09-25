package protocol

import (
	"encoding/binary"
	"errors"
)

// TimeSync implements an NTP-style offset probe over the audio UDP path so
// receivers can map sender timestamps onto their own monotonic clock.
const (
	TimeSyncRequest  uint8 = 1
	TimeSyncResponse uint8 = 2
)

const (
	timeSyncMagic  = "SVTS"
	TimeSyncSize   = 40
	timeSyncHeader = 8
)

// TimeSync carries four timestamps: t1 is the requester's send time, t2/t3
// the responder's receive/reply times, all in the same clock as the audio
// header TimestampNs on the responder side. t4 (requester receive time) is
// filled in locally and never travels.
type TimeSync struct {
	Kind       uint8
	T1, T2, T3 uint64
}

func EncodeTimeSync(s TimeSync) []byte {
	b := make([]byte, TimeSyncSize)
	copy(b, timeSyncMagic)
	b[4] = Version
	b[5] = s.Kind
	binary.BigEndian.PutUint16(b[6:], 0)
	binary.BigEndian.PutUint64(b[8:], s.T1)
	binary.BigEndian.PutUint64(b[16:], s.T2)
	binary.BigEndian.PutUint64(b[24:], s.T3)
	return b
}

func DecodeTimeSync(b []byte) (TimeSync, error) {
	if len(b) != TimeSyncSize || string(b[:4]) != timeSyncMagic || b[4] != Version {
		return TimeSync{}, errors.New("invalid time sync datagram")
	}
	kind := b[5]
	if kind != TimeSyncRequest && kind != TimeSyncResponse {
		return TimeSync{}, errors.New("unknown time sync kind")
	}
	return TimeSync{Kind: kind, T1: binary.BigEndian.Uint64(b[8:]), T2: binary.BigEndian.Uint64(b[16:]), T3: binary.BigEndian.Uint64(b[24:])}, nil
}
