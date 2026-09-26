package main

import (
	"github.com/wailsapp/wails/v2/pkg/runtime"
	"nexus-desktop/internal/discovery"
)

func (a *App) DiscoverDevices() ([]Device, error) {
	if a.discoverer != nil {
		a.discoverer.Close()
	}
	b := discovery.NewBrowser(
		func(d discovery.Device) {
			runtime.EventsEmit(a.ctx, "device:found", Device{
				Name:             d.Name,
				Host:             d.Host,
				Port:             d.Port,
				ID:               d.ID,
				Codec:            d.Codec,
				SampleRate:       d.SampleRate,
				Channels:         d.Channels,
				Bitrate:          d.Bitrate,
				FrameMs:          d.FrameMs,
				SupportedFrameMs: d.SupportedFrameMs,
				UpdatedAtMs:      d.UpdatedAtMs,
				SettingsDeviceID: d.SettingsDeviceID,
			})
		},
		func(deviceID string) {
			runtime.EventsEmit(a.ctx, "device:lost", deviceID)
		},
	)
	a.discoverer = b
	return nil, b.Start(a.ctx)
}
