package stream

import (
	"crypto/rand"
	"encoding/binary"
	"fmt"
	"net"
	"steamvoice-desktop/internal/protocol"
	"sync"
	"time"
)

const minBitrate = 48000
const connectRequestInterval = 1500 * time.Millisecond

type Sender struct {
	conn          *net.UDPConn
	session, seq  uint32
	bitrate       uint32
	frameMs       uint16
	mu            sync.Mutex
	feedbackDone  chan struct{}
	closeOnce     sync.Once
	feedbackWG    sync.WaitGroup
	onBitrate     func(int)
	onFeedback    func(protocol.ReceiverFeedback)
	lastFeedback  time.Time
	connResult    chan protocol.ConnControl
	created       time.Time
	now           func() uint64
	heartbeatSeq  uint32
	lastHeartbeat time.Time
	connNonce     uint64
}

func NewSender(address string, args ...int) (*Sender, error) {
	a, e := net.ResolveUDPAddr("udp", address)
	if e != nil {
		return nil, e
	}
	c, e := net.DialUDP("udp", nil, a)
	if e != nil {
		return nil, e
	}
	var raw [4]byte
	_, _ = rand.Read(raw[:])
	br := 128000
	if len(args) > 0 && args[0] > 0 {
		br = args[0]
	}
	frameMs := 10
	if len(args) > 1 && args[1] > 0 {
		frameMs = args[1]
	}
	if frameMs != 10 && frameMs != 20 {
		_ = c.Close()
		return nil, fmt.Errorf("unsupported frame duration: %d ms", frameMs)
	}
	now := time.Now()
	s := &Sender{conn: c, session: binary.BigEndian.Uint32(raw[:]), bitrate: uint32(br), frameMs: uint16(frameMs), feedbackDone: make(chan struct{}), lastFeedback: now, lastHeartbeat: now, connResult: make(chan protocol.ConnControl, 1), created: now}
	s.feedbackWG.Add(1)
	go s.feedbackLoop()
	return s, nil
}

// SetClock installs the stream timebase used for audio timestamps and
// time-sync responses. Without it the sender counts from its own creation.
func (s *Sender) SetClock(fn func() uint64) {
	s.mu.Lock()
	s.now = fn
	s.mu.Unlock()
}

func (s *Sender) clockNow() uint64 {
	s.mu.Lock()
	fn := s.now
	s.mu.Unlock()
	if fn != nil {
		return fn()
	}
	return uint64(time.Since(s.created))
}

func (s *Sender) SetBitrateCallback(fn func(int)) { s.mu.Lock(); s.onBitrate = fn; s.mu.Unlock() }

// SetFeedbackCallback installs a listener invoked for every valid feedback
// report, carrying the receiver's calibration progress.
func (s *Sender) SetFeedbackCallback(fn func(protocol.ReceiverFeedback)) {
	s.mu.Lock()
	s.onFeedback = fn
	s.mu.Unlock()
}

// FeedbackIdle reports how long ago the last valid receiver feedback arrived,
// counted from sender creation when no feedback has been received yet.
func (s *Sender) FeedbackIdle() time.Duration {
	s.mu.Lock()
	defer s.mu.Unlock()
	return time.Since(s.lastFeedback)
}

// LocalAddr reports the UDP source address feedback should be sent to.
func (s *Sender) LocalAddr() net.Addr { return s.conn.LocalAddr() }

func (s *Sender) ConnNonce() uint64 { s.mu.Lock(); defer s.mu.Unlock(); return s.connNonce }

func (s *Sender) SendSettings(settings protocol.Settings) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	_, err := s.conn.Write(protocol.EncodeSettings(settings))
	return err
}

// RequestConnection asks the receiver for permission to stream, retransmitting
// the request until it answers or the timeout elapses. A cached answer that
// raced the first wait is honored too.
func (s *Sender) RequestConnection(selfID, selfName string, timeout time.Duration) bool {
	s.mu.Lock()
	s.connNonce = protocol.NewConnNonce()
	nonce := s.connNonce
	s.mu.Unlock()
	req, err := protocol.EncodeConn(protocol.ConnControl{Kind: protocol.ConnRequest, DeviceID: selfID, Name: selfName, Nonce: nonce})
	if err != nil {
		return false
	}
	deadline := time.Now().Add(timeout)
	for {
		if _, err := s.conn.Write(req); err != nil {
			return false
		}
		wait := connectRequestInterval
		if remaining := time.Until(deadline); remaining <= 0 {
			return false
		} else if remaining < wait {
			wait = remaining
		}
		select {
		case r := <-s.connResult:
			return r.Allow && (r.Nonce == nonce || r.Nonce == 0)
		case <-s.feedbackDone:
			return false
		case <-time.After(wait):
		}
	}
}

func (s *Sender) feedbackLoop() {
	defer s.feedbackWG.Done()
	buf := make([]byte, 256)
	for {
		if time.Since(s.lastHeartbeat) >= time.Duration(protocol.HeartbeatIntervalMs)*time.Millisecond {
			s.lastHeartbeat = time.Now()
			_, _ = s.conn.Write(protocol.EncodeHeartbeat(protocol.Heartbeat{Kind: protocol.HeartbeatPing, Session: s.session, Sequence: s.heartbeatSeq, TimestampNs: s.clockNow()}))
			s.heartbeatSeq++
		}
		s.conn.SetReadDeadline(time.Now().Add(200 * time.Millisecond))
		n, _, err := s.conn.ReadFromUDP(buf)
		if err != nil {
			select {
			case <-s.feedbackDone:
				return
			default:
			}
			continue
		}
		if c, err := protocol.DecodeConn(buf[:n]); err == nil && c.Kind == protocol.ConnResponse {
			s.mu.Lock()
			nonce := s.connNonce
			s.mu.Unlock()
			// Zero is the pre-nonce wire format used by older peers; retain
			// interoperability for responses while all new sessions use nonce.
			if c.Nonce != nonce && c.Nonce != 0 {
				continue
			}
			select {
			case s.connResult <- c:
			default:
			}
			continue
		}
		if ts, err := protocol.DecodeTimeSync(buf[:n]); err == nil && ts.Kind == protocol.TimeSyncRequest {
			t2 := s.clockNow()
			resp := protocol.EncodeTimeSync(protocol.TimeSync{Kind: protocol.TimeSyncResponse, T1: ts.T1, T2: t2, T3: s.clockNow()})
			_, _ = s.conn.Write(resp)
			continue
		}
		if hb, err := protocol.DecodeHeartbeat(buf[:n]); err == nil {
			if hb.Session != s.session {
				continue
			}
			if hb.Kind == protocol.HeartbeatPing {
				_, _ = s.conn.Write(protocol.EncodeHeartbeat(protocol.Heartbeat{Kind: protocol.HeartbeatPong, Session: hb.Session, Sequence: hb.Sequence, TimestampNs: s.clockNow()}))
			}
			s.mu.Lock()
			s.lastFeedback = time.Now()
			s.mu.Unlock()
			continue
		}
		f, err := protocol.DecodeFeedback(buf[:n])
		if err != nil || f.Session != s.session {
			continue
		}
		s.mu.Lock()
		s.lastFeedback = time.Now()
		feedbackCB := s.onFeedback
		s.mu.Unlock()
		if feedbackCB != nil {
			feedbackCB(f)
		}
	}
}

// SendOpus sends one already encoded Opus frame stamped with tsNs, the
// capture time of the frame's first sample in the stream timebase.
func (s *Sender) SendOpus(tsNs uint64, opus []byte) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	b, e := protocol.Encode(protocol.Header{Codec: protocol.CodecOpus, Bitrate: s.bitrate, Session: s.session, Sequence: s.seq, FrameMilliseconds: s.frameMs, Flags: protocol.FlagFEC | protocol.FlagDTX, TimestampNs: tsNs}, opus)
	if e == nil {
		_, e = s.conn.Write(b)
		s.seq++
	}
	return e
}

// SendBye tells the receiver the session is over so its UI can disconnect
// immediately instead of waiting for the audio-silence timeout.
func (s *Sender) SendBye(selfID string) error {
	s.mu.Lock()
	nonce := s.connNonce
	s.mu.Unlock()
	b, err := protocol.EncodeConn(protocol.ConnControl{Kind: protocol.ConnBye, DeviceID: selfID, Nonce: nonce})
	if err != nil {
		return err
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	_, err = s.conn.Write(b)
	return err
}

func (s *Sender) Close() error {
	s.closeOnce.Do(func() {
		close(s.feedbackDone)
		_ = s.conn.Close()
	})
	s.feedbackWG.Wait()
	return nil
}
