package main

import (
	"embed"
	"log"

	"github.com/wailsapp/wails/v2"
	"github.com/wailsapp/wails/v2/pkg/options"
	"github.com/wailsapp/wails/v2/pkg/options/assetserver"
)

//go:embed all:frontend/dist
var assets embed.FS

func main() {
	app := NewApp()
	if err := wails.Run(&options.App{
		Title: "SyncTouch Audio",
		Width: 1120,
		Height: 720,
		AssetServer: &assetserver.Options{Assets: assets},
		OnStartup: app.Startup,
		OnShutdown: app.Shutdown,
		Bind: []interface{}{app},
	}); err != nil {
		log.Fatal(err)
	}
}
