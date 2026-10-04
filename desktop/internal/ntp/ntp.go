package ntp

import (
	"encoding/binary"
	"fmt"
	"net"
	"strings"
	"time"
)

const DefaultServer = "ntp.aliyun.com"

// Query returns the local clock minus the NTP server clock in nanoseconds.
func Query(server string, timeout time.Duration) (time.Duration, error) {
	server = strings.TrimSpace(server)
	if server == "" {
		server = DefaultServer
	}
	addr, err := net.ResolveUDPAddr("udp", net.JoinHostPort(server, "123"))
	if err != nil {
		return 0, err
	}
	c, err := net.DialUDP("udp", nil, addr)
	if err != nil {
		return 0, err
	}
	defer c.Close()
	var p [48]byte
	p[0] = 0x1b
	t1 := time.Now()
	if _, err = c.Write(p[:]); err != nil {
		return 0, err
	}
	_ = c.SetReadDeadline(time.Now().Add(timeout))
	if _, err = c.Read(p[:]); err != nil {
		return 0, err
	}
	t4 := time.Now()
	if binary.BigEndian.Uint32(p[40:44]) == 0 {
		return 0, fmt.Errorf("invalid NTP response")
	}
	sec := int64(binary.BigEndian.Uint32(p[40:44])) - 2208988800
	frac := int64(binary.BigEndian.Uint32(p[44:48]))
	serverTime := time.Unix(sec, frac*1_000_000_000/(1<<32))
	return t1.Add(t4.Sub(t1) / 2).Sub(serverTime), nil
}
