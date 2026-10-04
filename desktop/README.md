# Nexus Desktop

Windows Wails desktop sender for Nexus Android receivers. It discovers `_nexus._udp.local.` services, captures the default Windows render endpoint through WASAPI loopback, encodes 48 kHz stereo audio as 10 ms Opus frames, and sends them through UDP.

## Development

Install a Windows C/C++ compiler (Visual Studio Build Tools with the Desktop C++ workload, or MinGW-w64), libopus development headers/library, and `pkg-config`. The WASAPI backend and Opus encoder require CGO.

```powershell
$env:CGO_ENABLED = "1"
cd frontend; npm install; npm run build; cd ..
wails dev -tags "nexus_opus nolibopusfile"
```

Use `build.bat` for a production Windows installer. The script generates an NSIS installer for all users, which installs Nexus under `Program Files` and adds normal Windows uninstall metadata. The installation requires administrator permission.

When launched by double-click, `build.bat` keeps the window open so dependency or build errors remain visible. Use `build.bat --no-pause` when invoking it from another script or CI job.

The installer build requires NSIS (`makensis`) on `PATH` in addition to the compiler and libopus prerequisites. Build Wails from a terminal that has the C++ compiler and libopus environment loaded. Without the `nexus_opus` build tag the app intentionally reports that the encoder is unavailable.
