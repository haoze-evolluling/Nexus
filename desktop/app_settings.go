package main

import (
	"strings"
	"time"

	"nexus-desktop/internal/config"
	"nexus-desktop/internal/ntp"
)

func (a *App) GetNTPSettings() NTPStatus {
	return NTPStatus{Server: a.store.NTPServerName()}
}

func (a *App) SaveNTPServer(server string) error {
	server = strings.TrimSpace(server)
	if server == "" {
		server = ntp.DefaultServer
	}
	if len(server) > 253 || strings.ContainsAny(server, " \t\r\n") {
		return svErr("err_ntp_server", "")
	}
	return a.store.SetNTPServer(server)
}

func (a *App) TestNTPServer() NTPStatus {
	server := a.store.NTPServerName()
	offset, err := ntp.Query(server, time.Second)
	return NTPStatus{Server: server, OffsetMs: offset.Milliseconds(), Reachable: err == nil}
}

// ListAuthorizedDevices returns the remembered receivers that may connect
// without a confirmation prompt.
func (a *App) ListAuthorizedDevices() []config.AuthorizedDevice {
	return a.store.List()
}

// RemoveAuthorizedDevice drops a remembered receiver so its next connection
// asks for confirmation again.
func (a *App) RemoveAuthorizedDevice(deviceID string) error {
	return a.store.Remove(deviceID)
}
