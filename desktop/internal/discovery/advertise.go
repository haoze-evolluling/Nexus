package discovery

import (
	"github.com/grandcat/zeroconf"
)

// ServiceType is the mDNS service both platforms advertise; the TXT role
// attribute distinguishes senders (PCs) from speakers (Android receivers).
const ServiceType = "_steamvoice._udp"

// Advertiser publishes this desktop on the LAN so Android receivers can
// discover it and initiate connections.
type Advertiser struct {
	server *zeroconf.Server
}

func Advertise(instance, deviceID string, port int) (*Advertiser, error) {
	server, err := zeroconf.Register(instance, ServiceType, "local.", port, []string{
		"role=pc",
		"device_id=" + deviceID,
	}, nil)
	if err != nil {
		return nil, err
	}
	return &Advertiser{server: server}, nil
}

func (a *Advertiser) Close() { a.server.Shutdown() }
