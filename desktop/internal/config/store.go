package config

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
	"time"
)

// AuthorizedDevice remembers a peer the user chose to trust, keyed by the
// stable device identity peers carry in their connection requests (never by
// IP, which changes across networks and DHCP leases).
type AuthorizedDevice struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	AddedAtMs int64  `json:"addedAtMs"`
}

// File persists the desktop identity and the trusted-peer list so both stay
// stable across restarts.
type File struct {
	mu         sync.Mutex
	path       string
	DeviceID   string             `json:"deviceId"`
	DeviceName string             `json:"deviceName"`
	Authorized []AuthorizedDevice `json:"authorized"`
	NTPServer  string             `json:"ntpServer,omitempty"`
}

func (f *File) NTPServerName() string {
	f.mu.Lock()
	defer f.mu.Unlock()
	if f.NTPServer == "" {
		return "ntp.aliyun.com"
	}
	return f.NTPServer
}
func (f *File) SetNTPServer(server string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.NTPServer = server
	return f.save()
}

// Load reads (or creates) the config file at path. The identity is generated
// once and then reused forever.
func Load(path string) (*File, error) {
	f := &File{path: path}
	raw, err := os.ReadFile(path)
	switch {
	case err == nil:
		if err := json.Unmarshal(raw, f); err != nil {
			return nil, err
		}
		if f.DeviceID == "" {
			f.DeviceID = newIdentity()
		}
		if len(f.Authorized) == 0 {
			f.Authorized = []AuthorizedDevice{}
		}
		return f, f.save()
	case os.IsNotExist(err):
		f.DeviceID = newIdentity()
		f.Authorized = []AuthorizedDevice{}
		return f, f.save()
	default:
		return nil, err
	}
}

// Memory returns a non-persistent store with a fresh identity, used when the
// config directory is unavailable and in tests.
func Memory() *File {
	return &File{DeviceID: newIdentity(), Authorized: []AuthorizedDevice{}}
}

func DefaultPath() (string, error) {
	base, err := os.UserConfigDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(base, "SteamVoice", "desktop.json"), nil
}

func newIdentity() string {
	var raw [16]byte
	if _, err := rand.Read(raw[:]); err != nil {
		// crypto/rand failing is effectively fatal; a time-seeded fallback
		// still yields a unique-enough identity for this install.
		now := time.Now().UnixNano()
		for i := range raw {
			raw[i] = byte(now >> (uint(i%8) * 8))
		}
	}
	return hex.EncodeToString(raw[:])
}

func (f *File) save() error {
	if f.path == "" {
		return nil
	}
	raw, err := json.MarshalIndent(f, "", "  ")
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(f.path), 0o755); err != nil {
		return err
	}
	return os.WriteFile(f.path, raw, 0o644)
}

// Name reports the advertised friendly name under lock, since SetName may
// run concurrently.
func (f *File) Name() string {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.DeviceName
}

// SetName updates the advertised friendly name, persisting immediately.
func (f *File) SetName(name string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.DeviceName = name
	return f.save()
}

func (f *File) IsAuthorized(deviceID string) bool {
	f.mu.Lock()
	defer f.mu.Unlock()
	return findAuthorized(f.Authorized, deviceID) >= 0
}

// Authorize records a trusted peer, refreshing the stored name when the same
// identity reconnects with a new friendly name.
func (f *File) Authorize(deviceID, name string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	if i := findAuthorized(f.Authorized, deviceID); i >= 0 {
		f.Authorized[i].Name = name
		return f.save()
	}
	f.Authorized = append(f.Authorized, AuthorizedDevice{ID: deviceID, Name: name, AddedAtMs: time.Now().UnixMilli()})
	return f.save()
}

func (f *File) Remove(deviceID string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	if i := findAuthorized(f.Authorized, deviceID); i >= 0 {
		f.Authorized = append(f.Authorized[:i], f.Authorized[i+1:]...)
		return f.save()
	}
	return nil
}

func (f *File) List() []AuthorizedDevice {
	f.mu.Lock()
	defer f.mu.Unlock()
	out := make([]AuthorizedDevice, len(f.Authorized))
	copy(out, f.Authorized)
	return out
}

func findAuthorized(list []AuthorizedDevice, deviceID string) int {
	for i, d := range list {
		if d.ID == deviceID {
			return i
		}
	}
	return -1
}
