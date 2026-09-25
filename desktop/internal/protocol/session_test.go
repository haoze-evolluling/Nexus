package protocol

import "testing"

func TestConnectionStateTransitions(t *testing.T) {
	if got := NextState(StateConnected, EventHeartbeatTimeout); got != StateReconnecting {
		t.Fatalf("heartbeat timeout => %v", got)
	}
	if got := NextState(StateReconnecting, EventAuthorized); got != StateConnected {
		t.Fatalf("reconnect authorization => %v", got)
	}
	if got := NextState(StateConnected, EventConnect); got != StateConnected {
		t.Fatalf("invalid event changed state: %v", got)
	}
}

func TestHeartbeatRoundTrip(t *testing.T) {
	b, err := DecodeHeartbeat(EncodeHeartbeat(Heartbeat{Kind: HeartbeatPing, Session: 7, Sequence: 9, TimestampNs: 11}))
	if err != nil || b.Session != 7 || b.Sequence != 9 || b.TimestampNs != 11 {
		t.Fatalf("heartbeat round trip: %#v %v", b, err)
	}
}
