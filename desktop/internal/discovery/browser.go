package discovery

import (
	"context"
	"log"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/grandcat/zeroconf"
)

type Device struct {
	Name, Host       string
	Port             int
	ID               string
	Codec            string
	SampleRate       int
	Channels         int
	Bitrate          int
	FrameMs          int
	SupportedFrameMs []int
	UpdatedAtMs      int64
	SettingsDeviceID string
}

// SupportsOpus reports whether the receiver advertised the v2 codec.
func (d Device) SupportsOpus() bool { return strings.EqualFold(d.Codec, "opus") }

// grandcat/zeroconf stops re-querying after the first answer and discards
// partially resolved entries, so one browse run reliably surfaces only one of
// several receivers. The browser therefore runs short query cycles back to
// back: every cycle sends a fresh mDNS query, so each live receiver answers
// and the registry below stays complete.
const (
	browseWindow = 2 * time.Second
	browseEvery  = 3 * time.Second
	lostAfter    = 12 * time.Second
)

type Browser struct {
	onDevice func(Device)
	onLost   func(deviceID string)
	cancel   context.CancelFunc
	once     sync.Once
}

// NewBrowser reports devices through onDevice as they answer queries and
// forgets them through onLost once they stop answering.
func NewBrowser(onDevice func(Device), onLost func(string)) *Browser {
	return &Browser{onDevice: onDevice, onLost: onLost}
}

func (b *Browser) Start(parent context.Context) error {
	ctx, cancel := context.WithCancel(parent)
	b.cancel = cancel
	go b.loop(ctx)
	return nil
}

func (b *Browser) Close() {
	b.once.Do(func() {
		if b.cancel != nil {
			b.cancel()
		}
	})
}

func (b *Browser) loop(ctx context.Context) {
	var mu sync.Mutex
	seen := map[string]time.Time{}
	stopSweeper := make(chan struct{})
	go func() {
		ticker := time.NewTicker(time.Second)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-stopSweeper:
				return
			case <-ticker.C:
				now := time.Now()
				var lost []string
				mu.Lock()
				for id, at := range seen {
					if now.Sub(at) > lostAfter {
						delete(seen, id)
						lost = append(lost, id)
					}
				}
				mu.Unlock()
				for _, id := range lost {
					if b.onLost != nil {
						b.onLost(id)
					}
				}
			}
		}
	}()
	for {
		b.browseOnce(ctx, func(d Device) {
			mu.Lock()
			seen[d.ID] = time.Now()
			mu.Unlock()
			b.onDevice(d)
		})
		select {
		case <-ctx.Done():
			close(stopSweeper)
			return
		case <-time.After(browseEvery - browseWindow):
		}
	}
}

// browseOnce runs a single resolver cycle for the duration of browseWindow.
// The zeroconf client closes its entries channel when the cycle context ends,
// so the consumer goroutine terminates with it.
func (b *Browser) browseOnce(ctx context.Context, emit func(Device)) {
	resolver, err := zeroconf.NewResolver()
	if err != nil {
		log.Printf("mdns resolver unavailable: %v", err)
		time.Sleep(browseEvery)
		return
	}
	cycleCtx, cancel := context.WithTimeout(ctx, browseWindow)
	defer cancel()
	entries := make(chan *zeroconf.ServiceEntry, 16)
	go func() {
		for e := range entries {
			if device, ok := parseEntry(e); ok {
				emit(device)
			}
		}
	}()
	if err := resolver.Browse(cycleCtx, ServiceType, "local.", entries); err != nil {
		log.Printf("mdns browse failed: %v", err)
		return
	}
	<-cycleCtx.Done()
}

func parseEntry(e *zeroconf.ServiceEntry) (Device, bool) {
	host := e.HostName
	if len(e.AddrIPv4) > 0 {
		host = e.AddrIPv4[0].String()
	}
	name := strings.TrimSpace(e.Instance)
	if name == "" {
		name = strings.TrimSuffix(strings.TrimSpace(e.HostName), ".")
	}
	// Android prefixes its mDNS instance with the service name. Keep that
	// protocol marker out of the user-facing device name, matching Android's
	// own discovery display.
	name = strings.TrimPrefix(name, "SteamVoice-")
	// DNS-SD escapes spaces in instance names as "\\ "; expose the friendly
	// name rather than the wire-format escape sequence.
	name = strings.ReplaceAll(name, `\ `, " ")
	values := map[string]string{}
	for _, raw := range e.Text {
		p := strings.SplitN(raw, "=", 2)
		if len(p) == 2 {
			values[strings.ToLower(p[0])] = p[1]
		}
	}
	if strings.EqualFold(values["role"], "pc") {
		return Device{}, false
	}
	if !strings.EqualFold(values["codec"], "opus") {
		return Device{}, false
	}
	number := func(k string, fallback int) int {
		if n, err := strconv.Atoi(values[k]); err == nil && n > 0 {
			return n
		}
		return fallback
	}
	frameOptions := []int{}
	for _, raw := range strings.Split(values["frame_ms"], ",") {
		if n, err := strconv.Atoi(strings.TrimSpace(raw)); err == nil && (n == 10 || n == 20) {
			frameOptions = append(frameOptions, n)
		}
	}
	if len(frameOptions) == 0 {
		frameOptions = []int{10}
	}
	currentFrame := number("current_frame_ms", frameOptions[0])
	if currentFrame != 10 && currentFrame != 20 {
		currentFrame = frameOptions[0]
	}
	updatedAt, _ := strconv.ParseInt(values["settings_updated_at"], 10, 64)
	// Prefer the receiver's stable identity over the mDNS instance
	// name, which gets conflict suffixes like "(2)" and would fork
	// sessions for the same physical device.
	// device_id is the receiver identity. settings_device_id is metadata for
	// audio-settings conflict resolution and must not become the discovery key.
	id := values["device_id"]
	if id == "" {
		id = values["settings_device_id"]
	}
	if id == "" {
		id = e.Instance
	}
	return Device{Name: name, Host: host, Port: e.Port, ID: id, Codec: values["codec"], SampleRate: number("sample_rate", 48000), Channels: number("channels", 2), Bitrate: number("bitrate", 128000), FrameMs: currentFrame, SupportedFrameMs: frameOptions, UpdatedAtMs: updatedAt, SettingsDeviceID: values["settings_device_id"]}, true
}
