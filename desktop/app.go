package main

import (
	"context"
	"log"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
	"nexus-desktop/internal/capture"
	"nexus-desktop/internal/config"
	"nexus-desktop/internal/discovery"
	"nexus-desktop/internal/gateway"
	"nexus-desktop/internal/protocol"
)

type App struct {
	ctx             context.Context
	mu              sync.Mutex
	discoverer      *discovery.Browser
	advertiser      *discovery.Advertiser
	sessions        map[string]*deviceSession
	capture         *capture.Loopback
	frameMs         int
	staleAfter      time.Duration
	dropAfter       time.Duration
	requestTimeout  time.Duration
	store           *config.File
	listener        *gateway.Listener
	pending         map[string]*pendingRequest
	pendingByDevice map[string]string
	connecting      map[string]bool
	localBitrate    int
	localFrameMs    int
	clockAnchor     time.Time
	lastPCM         time.Time
}

func NewApp() *App {
	var store *config.File
	if path, err := config.DefaultPath(); err == nil {
		if loaded, err := config.Load(path); err == nil {
			store = loaded
		} else {
			log.Printf("config load failed, using in-memory identity: %v", err)
		}
	}
	if store == nil {
		store = config.Memory()
	}
	return NewAppWithStore(store)
}

// NewAppWithStore wires an explicit identity/trust store; tests use it to
// avoid touching the real config file.
func NewAppWithStore(store *config.File) *App {
	return &App{
		sessions:        map[string]*deviceSession{},
		staleAfter:      feedbackStaleAfter,
		dropAfter:       feedbackDropAfter,
		requestTimeout:  requestExpiry,
		pending:         map[string]*pendingRequest{},
		pendingByDevice: map[string]string{},
		connecting:      map[string]bool{},
		localBitrate:    128000,
		localFrameMs:    10,
		store:           store,
	}
}

func (a *App) Startup(ctx context.Context) {
	a.ctx = ctx
	if a.store.Name() == "" {
		if host, err := os.Hostname(); err == nil && strings.TrimSpace(host) != "" {
			_ = a.store.SetName(strings.TrimSpace(host))
		}
	}
	listener, err := gateway.Start(protocol.DesktopControlPort, a.store.DeviceID, a.onConnRequest, a.onConnBye)
	if err != nil {
		// The app still works as a sender; only inbound connections break.
		log.Printf("control listener unavailable (another instance running?): %v", err)
	} else {
		a.listener = listener
	}
	if advertiser, err := discovery.Advertise("Nexus-"+a.pcName(), a.store.DeviceID, protocol.DesktopControlPort); err != nil {
		log.Printf("mDNS advertise failed: %v", err)
	} else {
		a.advertiser = advertiser
	}
	go a.monitorLoop(ctx)
	go a.keepaliveLoop(ctx)
}

func (a *App) pcName() string {
	if name := a.store.Name(); name != "" {
		return name
	}
	return "PC"
}

func (a *App) Shutdown(context.Context) {
	a.mu.Lock()
	sessions := a.sessions
	c := a.capture
	a.sessions = map[string]*deviceSession{}
	a.capture = nil
	a.frameMs = 0
	listener := a.listener
	advertiser := a.advertiser
	a.listener = nil
	a.advertiser = nil
	for _, entry := range a.pending {
		entry.timer.Stop()
	}
	a.pending = map[string]*pendingRequest{}
	a.pendingByDevice = map[string]string{}
	a.mu.Unlock()
	if advertiser != nil {
		advertiser.Close()
	}
	if listener != nil {
		listener.Close()
	}
	if c != nil {
		c.Close()
	}
	for _, s := range sessions {
		_ = s.sender.SendBye(a.store.DeviceID)
		_ = s.sender.Close()
	}
}

// GetIdentity exposes the stable desktop identity used for authorization.
func (a *App) GetIdentity() Identity {
	return Identity{DeviceID: a.store.DeviceID, Name: a.pcName()}
}

func (a *App) emitStatus(status DeviceStatus) {
	a.mu.Lock()
	ctx := a.ctx
	a.mu.Unlock()
	if ctx != nil {
		runtime.EventsEmit(ctx, "stream:status", status)
	}
}
