package protocol

import (
	"encoding/binary"
	"errors"
)

// ConnectionState is the only state vocabulary used by the transport layer.
// UI states must be derived from these values, never inferred from packets.
type ConnectionState uint8

const (
	StateIdle ConnectionState = iota
	StateConnecting
	StateAwaitingAuthorization
	StateConnected
	StateDisconnecting
	StateReconnecting
	StateFailed
)

type ConnectionEvent uint8

const (
	EventConnect ConnectionEvent = iota + 1
	EventRequestReceived
	EventAuthorized
	EventDenied
	EventHandshakeTimeout
	EventHeartbeatTimeout
	EventRemoteBye
	EventLocalDisconnect
	EventRetry
)

// NextState is deliberately table-like and side-effect free. A caller owns
// one machine per peer and serializes calls, preventing parallel reconnects.
func NextState(state ConnectionState, event ConnectionEvent) ConnectionState {
	switch state {
	case StateIdle:
		if event == EventConnect || event == EventRequestReceived {
			return StateConnecting
		}
	case StateConnecting:
		switch event {
		case EventAuthorized:
			return StateConnected
		case EventRequestReceived:
			return StateAwaitingAuthorization
		case EventDenied, EventHandshakeTimeout:
			return StateFailed
		}
	case StateAwaitingAuthorization:
		switch event {
		case EventAuthorized:
			return StateConnected
		case EventDenied, EventHandshakeTimeout:
			return StateFailed
		}
	case StateConnected:
		switch event {
		case EventLocalDisconnect, EventRemoteBye:
			return StateDisconnecting
		case EventHeartbeatTimeout:
			return StateReconnecting
		}
	case StateDisconnecting:
		if event == EventRetry {
			return StateIdle
		}
	case StateReconnecting:
		switch event {
		case EventAuthorized:
			return StateConnected
		case EventHandshakeTimeout:
			return StateFailed
		case EventLocalDisconnect:
			return StateDisconnecting
		}
	case StateFailed:
		if event == EventRetry || event == EventConnect {
			return StateConnecting
		}
	}
	return state
}

// Transport constants are shared by discovery, handshake and liveness code.
const (
	ProtocolVersion      = Version
	AudioPort            = ReceiverAudioPort
	ControlPort          = DesktopControlPort
	HandshakeTimeoutMs   = 8000
	RequestRetryMs       = 1500
	HeartbeatIntervalMs  = 1000
	HeartbeatTimeoutMs   = 3500
	MaxReconnectAttempts = 5
)

const heartbeatSize = 32

type Heartbeat struct {
	Kind              uint8
	Session, Sequence uint32
	TimestampNs       uint64
}

const (
	HeartbeatPing uint8 = 1
	HeartbeatPong uint8 = 2
)

func EncodeHeartbeat(h Heartbeat) []byte {
	b := make([]byte, heartbeatSize)
	copy(b, "SVHB")
	b[4] = Version
	b[5] = h.Kind
	binary.BigEndian.PutUint32(b[8:], h.Session)
	binary.BigEndian.PutUint32(b[12:], h.Sequence)
	binary.BigEndian.PutUint64(b[16:], h.TimestampNs)
	return b
}
func DecodeHeartbeat(b []byte) (Heartbeat, error) {
	if len(b) != heartbeatSize || string(b[:4]) != "SVHB" || b[4] != Version || (b[5] != HeartbeatPing && b[5] != HeartbeatPong) {
		return Heartbeat{}, errors.New("invalid heartbeat")
	}
	return Heartbeat{Kind: b[5], Session: binary.BigEndian.Uint32(b[8:]), Sequence: binary.BigEndian.Uint32(b[12:]), TimestampNs: binary.BigEndian.Uint64(b[16:])}, nil
}
