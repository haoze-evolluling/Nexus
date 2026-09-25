package protocol

import (
	"bytes"
	"crypto/rand"
	"encoding/binary"
	"errors"
	"strings"
	"time"
	"unicode/utf8"
)

// ConnControl messages ride the same UDP transport as audio. The side that
// wants a session sends ConnRequest and waits for ConnResponse; the responder
// may gate the request behind user authorization. ConnBye ends an established
// session so the peer can update its state immediately instead of waiting for
// a feedback timeout.
const (
	ConnRequest  uint8 = 1
	ConnResponse uint8 = 2
	ConnBye      uint8 = 3
)

const (
	connMagic              = "SVCR"
	connHeaderSize         = 8
	connNonceSize          = 8
	MaxDeviceIDLen         = 64
	MaxDeviceNameLen       = 64
	connAllow        uint8 = 1
	connDeny         uint8 = 2
)

type ConnControl struct {
	Kind     uint8
	DeviceID string
	Name     string
	Allow    bool
	Nonce    uint64
}

func NewConnNonce() uint64 {
	var b [8]byte
	if _, err := rand.Read(b[:]); err != nil {
		return uint64(time.Now().UnixNano())
	}
	return binary.BigEndian.Uint64(b[:])
}

// EncodeConn marshals a connection control datagram. Request carries the
// requester's stable device ID plus a human-readable name; response carries
// the responder's device ID and the decision; bye carries only the sender's
// device ID.
func EncodeConn(c ConnControl) ([]byte, error) {
	id := c.DeviceID
	if len(id) > MaxDeviceIDLen {
		return nil, errors.New("device id exceeds connection control limit")
	}
	name := strings.TrimRight(c.Name, "\x00")
	if len(name) > MaxDeviceNameLen {
		// Cut on a rune boundary so the truncated name stays valid UTF-8.
		end := MaxDeviceNameLen
		for end > 0 && !utf8.RuneStart(name[end]) {
			end--
		}
		name = name[:end]
	}
	body := make([]byte, 0, connNonceSize+len(id)+1+len(name))
	var nonce [8]byte
	binary.BigEndian.PutUint64(nonce[:], c.Nonce)
	body = append(body, nonce[:]...)
	body = append(body, id...)
	switch c.Kind {
	case ConnRequest:
		body = append(body, 0)
		body = append(body, []byte(name)...)
	case ConnResponse:
		decision := connDeny
		if c.Allow {
			decision = connAllow
		}
		body = append(body, 0, decision)
	case ConnBye:
	default:
		return nil, errors.New("unknown connection control kind")
	}
	b := make([]byte, connHeaderSize+len(body))
	copy(b, connMagic)
	b[4] = Version
	b[5] = c.Kind
	binary.BigEndian.PutUint16(b[6:], 0)
	copy(b[connHeaderSize:], body)
	return b, nil
}

func DecodeConn(b []byte) (ConnControl, error) {
	if len(b) < connHeaderSize || string(b[:4]) != connMagic || b[4] != Version {
		return ConnControl{}, errors.New("invalid connection control")
	}
	c := ConnControl{Kind: b[5]}
	bodyOffset := connHeaderSize
	if len(b) >= connHeaderSize+connNonceSize {
		c.Nonce = binary.BigEndian.Uint64(b[connHeaderSize:])
		bodyOffset += connNonceSize
	}
	// Accept legacy response/bye packets whose body starts immediately after
	// the header. New responses have an explicit nonce followed by id NUL.
	if c.Kind == ConnResponse {
		if i := bytes.IndexByte(b[connHeaderSize:], 0); i >= 1 && connHeaderSize+i+2 == len(b) && (b[connHeaderSize+i+1] == connAllow || b[connHeaderSize+i+1] == connDeny) {
			bodyOffset = connHeaderSize
			c.Nonce = 0
		}
	}
	body := b[bodyOffset:]
	switch c.Kind {
	case ConnRequest:
		deviceID, name, ok := splitConnField(body)
		if !ok {
			return ConnControl{}, errors.New("malformed connection request")
		}
		c.DeviceID, c.Name = deviceID, name
	case ConnResponse:
		deviceID, decision, ok := splitConnField(body)
		if !ok || len(decision) != 1 {
			return ConnControl{}, errors.New("malformed connection response")
		}
		c.DeviceID = deviceID
		switch decision[0] {
		case connAllow:
			c.Allow = true
		case connDeny:
			c.Allow = false
		default:
			return ConnControl{}, errors.New("unknown connection decision")
		}
	case ConnBye:
		if len(body) > MaxDeviceIDLen || strings.ContainsRune(string(body), 0) {
			return ConnControl{}, errors.New("malformed connection bye")
		}
		c.DeviceID = string(body)
	default:
		return ConnControl{}, errors.New("unknown connection control kind")
	}
	if len(c.DeviceID) > MaxDeviceIDLen || len(c.DeviceID) == 0 {
		return ConnControl{}, errors.New("invalid device id in connection control")
	}
	return c, nil
}

// splitConnField splits the payload at the first NUL and requires a non-empty
// first field.
func splitConnField(b []byte) (string, string, bool) {
	for i, v := range b {
		if v == 0 {
			if i == 0 {
				return "", "", false
			}
			return string(b[:i]), string(b[i+1:]), true
		}
	}
	return "", "", false
}
