# SteamVoice transport protocol v4

This document is the contract shared by Desktop and Android. All integers are
unsigned big-endian unless stated otherwise. Every packet is a complete UDP
datagram; packets are never concatenated or fragmented by the application.

## Endpoints and identity

* Android audio endpoint: UDP `40125` (`SV01`, feedback, settings, time sync,
  heartbeat, connection control and Android peer calibration control).
* Desktop control endpoint: UDP `40126` (`SVCR` requests, responses and bye).
* mDNS `_steamvoice._udp.local.` advertises the endpoint port and a stable
  `device_id`. The advertised role is `pc` or `speaker`; the role is not an
  authorization decision.
* Device IDs are UTF-8, 1..64 bytes and are the sole peer identity key.

## Connection lifecycle

Both implementations use the same states: `IDLE`, `CONNECTING`,
`AWAITING_AUTHORIZATION`, `CONNECTED`, `DISCONNECTING`, `RECONNECTING`, and
`FAILED`. A peer owns one serialized state machine and one session at a time.
An incoming request while a peer is pending is an idempotent retransmission;
it must not create another prompt or socket. A new session replaces the old
session only after the old one has entered `DISCONNECTING`.

`SVCR` request/response/bye packets are retransmitted every 1500 ms for at
most 8 s. Authorization is keyed by device ID. A successful response permits
audio for the newly generated session ID; a deny or timeout enters `FAILED`.
Explicit bye enters `DISCONNECTING` on both sides. Reconnect uses bounded,
serialized attempts (maximum five) with the same handshake rules.

## Liveness

`SVHB` is a 32-byte packet: magic (4), version (1), kind (1), reserved (2),
session (4), sequence (4), monotonic timestamp in nanoseconds (8), reserved
(8). Kind `1` is ping and `2` is pong. The sender emits a ping every 1000 ms;
the receiver replies with a pong echoing session and sequence. Missing valid
heartbeats for 3500 ms is an abnormal disconnect and enters `RECONNECTING`.
Audio silence is not used as the sole liveness signal.

## Time

`TimestampNs` fields and `SVTS` exchange fields always use each process's
monotonic nanosecond clock. They are never wall-clock values and are never
compared across devices without the NTP-style offset estimate. Wall-clock
milliseconds are reserved for user settings conflict resolution only.

## Data and extension rules

`SV01` audio packets carry a session and sequence. Receivers discard packets
from another device, another session, or an already-consumed sequence. Unknown
magic, version, kind, malformed lengths, and unsupported formats are dropped
without changing connection state. New message kinds must use a new magic or
an explicitly versioned layout; existing fields are not overloaded.

## Android peer calibration

`SVAC` datagrams are exchanged directly between two Android receivers on UDP
`40125`; the desktop never receives or relays them. The requester must first
receive explicit peer confirmation, then performs `SVTS` probes and sends each
device a reset target expressed in that device's monotonic clock. At the target
each receiver clears only its local audio/jitter queue, preserving the desktop
session and its existing clock mapping. Operations are rejected when the two
receivers are attached to different PCs or the peer has not confirmed them.
