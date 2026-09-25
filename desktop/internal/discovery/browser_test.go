package discovery

import (
	"testing"

	"github.com/grandcat/zeroconf"
)

func TestParseEntryStripsSteamVoiceNamePrefix(t *testing.T) {
	entry := &zeroconf.ServiceEntry{
		ServiceRecord: *zeroconf.NewServiceRecord(`SteamVoice-Xiaomi\ 2602BRT18C`, ServiceType, "local."),
		HostName:      "xiaomi.local.",
		Port:          40125,
		Text:          []string{"role=speaker", "codec=opus", "device_id=android-1"},
	}
	device, ok := parseEntry(entry)
	if !ok {
		t.Fatal("parseEntry rejected a valid Android advertisement")
	}
	if device.Name != "Xiaomi 2602BRT18C" {
		t.Fatalf("device name = %q, want %q", device.Name, "Xiaomi 2602BRT18C")
	}
}
