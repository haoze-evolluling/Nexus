package protocol

import (
	"encoding/binary"
	"errors"
)

const SettingsControlSize = 40

type Settings struct {
	BitrateKbps uint32
	FrameMs     uint16
	UpdatedAtMs int64
	DeviceID    string
}

func EncodeSettings(s Settings) []byte {
	b := make([]byte, SettingsControlSize)
	copy(b, "SVCS")
	b[4] = Version
	b[5] = 1
	binary.BigEndian.PutUint32(b[8:], s.BitrateKbps)
	binary.BigEndian.PutUint16(b[12:], s.FrameMs)
	binary.BigEndian.PutUint64(b[16:], uint64(s.UpdatedAtMs))
	copy(b[24:40], []byte(s.DeviceID))
	return b
}

func DecodeSettings(b []byte) (Settings, error) {
	if len(b) != SettingsControlSize || string(b[:4]) != "SVCS" || b[4] != Version || b[5] != 1 {
		return Settings{}, errors.New("invalid settings control")
	}
	id := string(b[24:40])
	for len(id) > 0 && id[len(id)-1] == 0 {
		id = id[:len(id)-1]
	}
	return Settings{BitrateKbps: binary.BigEndian.Uint32(b[8:]), FrameMs: binary.BigEndian.Uint16(b[12:]), UpdatedAtMs: int64(binary.BigEndian.Uint64(b[16:])), DeviceID: id}, nil
}

const FeedbackSize = 35

// Receiver sync states reported in feedback, driving the calibration UI.
const (
	SyncUnknown     = 0
	SyncCalibrating = 1 // receiving audio, clock offset still being measured
	SyncAligned     = 2 // clock offset converged, playback ramping up
	SyncPlaying     = 3 // synchronized playback running
)

type ReceiverFeedback struct {
	Session, HighestSeq, Received, Lost uint32
	Queue                               uint16
	Bitrate                             uint32
	// SyncState plus the receiver's clock-sync measurements (median offset
	// against the sender clock and exchange round-trip time).
	SyncState uint8
	OffsetMs  int16
	RttMs     uint16
}

func EncodeFeedback(f ReceiverFeedback) []byte {
	b := make([]byte, FeedbackSize)
	copy(b, "SVCT")
	b[4] = Version
	b[5] = 1
	binary.BigEndian.PutUint32(b[8:], f.Session)
	binary.BigEndian.PutUint32(b[12:], f.HighestSeq)
	binary.BigEndian.PutUint32(b[16:], f.Received)
	binary.BigEndian.PutUint32(b[20:], f.Lost)
	binary.BigEndian.PutUint16(b[24:], f.Queue)
	binary.BigEndian.PutUint32(b[26:], f.Bitrate)
	b[30] = f.SyncState
	binary.BigEndian.PutUint16(b[31:], uint16(f.OffsetMs))
	binary.BigEndian.PutUint16(b[33:], f.RttMs)
	return b
}
func DecodeFeedback(b []byte) (ReceiverFeedback, error) {
	if len(b) != FeedbackSize || string(b[:4]) != "SVCT" || b[4] != Version || b[5] != 1 {
		return ReceiverFeedback{}, errors.New("invalid feedback")
	}
	f := ReceiverFeedback{Session: binary.BigEndian.Uint32(b[8:]), HighestSeq: binary.BigEndian.Uint32(b[12:]), Received: binary.BigEndian.Uint32(b[16:]), Lost: binary.BigEndian.Uint32(b[20:]), Queue: binary.BigEndian.Uint16(b[24:]), Bitrate: binary.BigEndian.Uint32(b[26:])}
	f.SyncState = b[30]
	f.OffsetMs = int16(binary.BigEndian.Uint16(b[31:]))
	f.RttMs = binary.BigEndian.Uint16(b[33:])
	return f, nil
}
