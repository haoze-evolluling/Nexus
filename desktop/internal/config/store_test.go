package config

import (
	"path/filepath"
	"testing"
)

func TestLoadCreatesStableIdentity(t *testing.T) {
	path := filepath.Join(t.TempDir(), "nested", "desktop.json")
	first, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if len(first.DeviceID) != 32 {
		t.Fatalf("device id = %q", first.DeviceID)
	}
	again, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if again.DeviceID != first.DeviceID {
		t.Fatalf("identity changed across loads: %q vs %q", first.DeviceID, again.DeviceID)
	}
}

func TestAuthorizeIsIdempotentAndRemovable(t *testing.T) {
	f, err := Load(filepath.Join(t.TempDir(), "desktop.json"))
	if err != nil {
		t.Fatal(err)
	}
	if err := f.Authorize("dev-1", "Pixel 9"); err != nil {
		t.Fatal(err)
	}
	if err := f.Authorize("dev-1", "Pixel 9 (renamed)"); err != nil {
		t.Fatal(err)
	}
	list := f.List()
	if len(list) != 1 || list[0].Name != "Pixel 9 (renamed)" || !f.IsAuthorized("dev-1") {
		t.Fatalf("list=%+v", list)
	}
	if err := f.Remove("dev-1"); err != nil {
		t.Fatal(err)
	}
	if f.IsAuthorized("dev-1") || len(f.List()) != 0 {
		t.Fatal("device not removed")
	}
}

func TestAuthorizePersistsAcrossLoad(t *testing.T) {
	path := filepath.Join(t.TempDir(), "desktop.json")
	f, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if err := f.Authorize("dev-2", "Pad"); err != nil {
		t.Fatal(err)
	}
	reloaded, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if !reloaded.IsAuthorized("dev-2") {
		t.Fatal("authorization lost after reload")
	}
}

func TestNTPServerDefaultsAndPersists(t *testing.T) {
	path := filepath.Join(t.TempDir(), "desktop.json")
	f, err := Load(path)
	if err != nil || f.NTPServerName() != "ntp.aliyun.com" {
		t.Fatalf("default=%q err=%v", f.NTPServerName(), err)
	}
	if err := f.SetNTPServer("time.example"); err != nil {
		t.Fatal(err)
	}
	reloaded, err := Load(path)
	if err != nil || reloaded.NTPServerName() != "time.example" {
		t.Fatalf("server=%q err=%v", reloaded.NTPServerName(), err)
	}
}
