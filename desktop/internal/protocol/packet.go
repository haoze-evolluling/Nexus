package protocol

import (
	"encoding/binary"
	"errors"
)

const (
	Magic             = "SV01"
	Version           = 4
	CodecOpus         = 1
	HeaderSize        = 40
	SampleRate        = 48000
	Channels          = 2
	FrameMilliseconds = 20
	FlagFEC           = 1 << 0
	FlagDTX           = 1 << 1
	FlagControl       = 1 << 7
	// ReceiverAudioPort is the fixed UDP port Android receivers listen on
	// for audio, feedback and control datagrams.
	ReceiverAudioPort = 40125
	// DesktopControlPort is the fixed UDP port desktops listen on for
	// connection control from receivers.
	DesktopControlPort = 40126
)

// Header is the metadata carried by every v4 Opus datagram.
type Header struct {
	Codec             uint8
	SampleRate        uint32
	Channels          uint8
	Bitrate           uint32
	Session           uint32
	Sequence          uint32
	PayloadLength     uint16
	FrameMilliseconds uint16
	Flags             uint8
	// TimestampNs is the sender-clock capture time of the frame's first
	// sample, in nanoseconds since the sender's capture epoch. Receivers use
	// it to schedule multi-device synchronized playback.
	TimestampNs uint64
}

func Encode(h Header, opus []byte) ([]byte, error) {
	if len(opus) > 65535 {
		return nil, errors.New("opus frame exceeds UDP packet limit")
	}
	if h.Codec == 0 {
		h.Codec = CodecOpus
	}
	if h.SampleRate == 0 {
		h.SampleRate = SampleRate
	}
	if h.Channels == 0 {
		h.Channels = Channels
	}
	if h.FrameMilliseconds == 0 {
		h.FrameMilliseconds = FrameMilliseconds
	}
	b := make([]byte, HeaderSize+len(opus))
	copy(b, Magic)
	b[4] = Version
	b[5] = h.Codec
	binary.BigEndian.PutUint32(b[6:], h.SampleRate)
	b[10] = h.Channels
	binary.BigEndian.PutUint32(b[12:], h.Bitrate)
	binary.BigEndian.PutUint32(b[16:], h.Session)
	binary.BigEndian.PutUint32(b[20:], h.Sequence)
	binary.BigEndian.PutUint16(b[24:], uint16(len(opus)))
	binary.BigEndian.PutUint16(b[26:], h.FrameMilliseconds)
	b[28] = h.Flags
	binary.BigEndian.PutUint64(b[32:], h.TimestampNs)
	copy(b[HeaderSize:], opus)
	return b, nil
}

func Decode(b []byte) (Header, []byte, error) {
	if len(b) < HeaderSize || string(b[:4]) != Magic || b[4] != Version || b[5] != CodecOpus {
		return Header{}, nil, errors.New("invalid SteamVoice v4 packet")
	}
	h := Header{Codec: b[5], SampleRate: binary.BigEndian.Uint32(b[6:]), Channels: b[10], Bitrate: binary.BigEndian.Uint32(b[12:]), Session: binary.BigEndian.Uint32(b[16:]), Sequence: binary.BigEndian.Uint32(b[20:]), PayloadLength: binary.BigEndian.Uint16(b[24:]), FrameMilliseconds: binary.BigEndian.Uint16(b[26:]), Flags: b[28], TimestampNs: binary.BigEndian.Uint64(b[32:])}
	if h.SampleRate != SampleRate || h.Channels != Channels || (h.FrameMilliseconds != 10 && h.FrameMilliseconds != 20) {
		return Header{}, nil, errors.New("unsupported audio format")
	}
	n := int(h.PayloadLength)
	if len(b) != HeaderSize+n {
		return Header{}, nil, errors.New("invalid payload length")
	}
	return h, b[HeaderSize:], nil
}
