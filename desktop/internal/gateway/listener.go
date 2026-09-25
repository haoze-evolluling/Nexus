package gateway

import (
	"net"
	"sync"

	"steamvoice-desktop/internal/protocol"
)

// ControlPort is the fixed UDP port the desktop listens on for connection
// control datagrams (requests, responses, disconnects).
const ControlPort = 40126

// Peer identifies the remote side of a connection control datagram.
type Peer struct {
	DeviceID string
	Name     string
	Addr     *net.UDPAddr
	Nonce    uint64
}

// Listener receives connection control datagrams from receivers and lets the
// app answer them once the user (or the trust store) has decided.
type Listener struct {
	conn      *net.UDPConn
	selfID    string
	onRequest func(Peer)
	onBye     func(deviceID string, nonce uint64, addr *net.UDPAddr)
	closeOnce sync.Once
	wg        sync.WaitGroup
}

// Start binds the control port; port 0 picks an ephemeral port (tests).
// selfID is the identity stamped into outgoing responses.
func Start(port int, selfID string, onRequest func(Peer), onBye interface{}) (*Listener, error) {
	conn, err := net.ListenUDP("udp", &net.UDPAddr{IP: net.IPv4zero, Port: port})
	if err != nil {
		return nil, err
	}
	var bye func(string, uint64, *net.UDPAddr)
	switch fn := onBye.(type) {
	case func(string, uint64, *net.UDPAddr):
		bye = fn
	case func(string, *net.UDPAddr):
		bye = func(id string, _ uint64, addr *net.UDPAddr) { fn(id, addr) }
	}
	l := &Listener{conn: conn, selfID: selfID, onRequest: onRequest, onBye: bye}
	l.wg.Add(1)
	go l.loop()
	return l, nil
}

// Addr reports the bound UDP address.
func (l *Listener) Addr() *net.UDPAddr {
	return l.conn.LocalAddr().(*net.UDPAddr)
}

func (l *Listener) loop() {
	defer l.wg.Done()
	buf := make([]byte, 512)
	for {
		n, addr, err := l.conn.ReadFromUDP(buf)
		if err != nil {
			return
		}
		msg, err := protocol.DecodeConn(buf[:n])
		if err != nil {
			continue
		}
		peer := Peer{DeviceID: msg.DeviceID, Name: msg.Name, Addr: addr, Nonce: msg.Nonce}
		switch msg.Kind {
		case protocol.ConnRequest:
			if l.onRequest != nil {
				l.onRequest(peer)
			}
		case protocol.ConnBye:
			if l.onBye != nil {
				l.onBye(msg.DeviceID, msg.Nonce, addr)
			}
		}
	}
}

// Respond answers a pending request from peer.
func (l *Listener) Respond(peer Peer, allow bool) error {
	b, err := protocol.EncodeConn(protocol.ConnControl{Kind: protocol.ConnResponse, DeviceID: l.selfID, Allow: allow, Nonce: peer.Nonce})
	if err != nil {
		return err
	}
	_, err = l.conn.WriteToUDP(b, peer.Addr)
	return err
}

func (l *Listener) Close() {
	l.closeOnce.Do(func() { _ = l.conn.Close() })
	l.wg.Wait()
}
