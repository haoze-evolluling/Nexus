# Nexus

Nexus is an open-source, cross-platform control suite combining **driverless Bluetooth HID peripheral emulation** on Android with **ultra-low-latency LAN wireless audio streaming** from Windows.

- **Android Client**: A native Android application (Jetpack Compose + NDK/C++ Opus) that emulates standard Bluetooth HID input hardware (multitouch touchpad, customizable mechanical keyboard, dual-stick gamepad, and smart TV remote) alongside a synchronized low-latency Opus audio receiver.
- **Windows Desktop Sender**: A lightweight desktop background streamer (Go + Wails v2 + Vue 3) that captures system audio via WASAPI Loopback, compresses it in real time using libopus, and multicasts it over UDP to one or more Android devices with per-device channel routing and clock synchronization.

---

## Architecture Overview

```text
┌────────────────────────────────────────────────────────┐
│               Windows Desktop Streamer                 │
│  (Go / Wails v2 / Vue 3 / WASAPI Loopback / libopus)   │
└──────────────┬───────────────────────────▲─────────────┘
               │ UDP Audio (NX01)          │ Feedback (NXCT)
               │ Control / Handshake       │ TimeSync (NXTS)
               ▼                           │
┌──────────────────────────────────────────┴─────────────┐
│                 Android Client (Nexus)                 │
│         (Kotlin / Jetpack Compose / NDK libopus)        │
├────────────────────────────┬───────────────────────────┤
│    Audio Receiver Engine   │   Bluetooth HID Emulation  │
│  - UDP Jitter Buffer       │   - Touchpad (Gestures)   │
│  - NTP Clock Sync (NXTS)   │   - Mechanical Keyboard   │
│  - Peer Calibration (NXAC) │   - Dual-Stick Gamepad    │
│  - Background MediaSession │   - TV & Media Remote     │
│                            │   - AI Agent Console      │
└────────────────────────────┴─────────────┬─────────────┘
                                           │ Standard Bluetooth HID
                                           ▼
                                 ┌───────────────────┐
                                 │   Target Host     │
                                 │ (PC / TV / Console│
                                 │  No Drivers Req.) │
                                 └───────────────────┘
```

---

## Key Features

### 1. Bluetooth HID Peripheral Emulation (Android)

Nexus transforms your Android phone or tablet into standard Bluetooth HID hardware without requiring any client-side software or proprietary drivers on the target host.

- **Dual HID Enumeration Profiles**:
  - `KEYBOARD_MOUSE`: Keyboard Top-Level Collection (TLC) placed first; host enumerates as a composite keyboard and mouse.
  - `GAMEPAD`: Gamepad TLC placed first; recognized natively as a standard 6-axis 16-button HID Game Controller across Windows DirectInput/XInput, Linux, SDL, browser Gamepad API, and Android TV.
- **Virtual Multi-Touch Touchpad**:
  - Single-finger cursor tracking and single-tap (left click).
  - Two-finger scrolling and two-finger tap (right click).
  - Three-finger tap (middle click).
  - Long-press to drag with anti-jitter thresholds.
  - Configurable sensitivity (1–10 scale), cursor speed, natural/inverted scroll direction, and on-screen hardware-style click buttons.
- **Mechanical Keyboard & Switch Acoustic Synthesizer**:
  - Full on-screen keyboard layout with modifier keys (Ctrl, Shift, Alt, GUI) and 6-key rollover.
  - Built-in real-time physical acoustic synthesizer (`KeyboardSoundSynthesizer`) generating authentic switch sound profiles:
    - *Tactile / Clicky / Linear*: Cherry MX Browns, Cherry MX Blues, Cherry MX Blacks, Holy Pandas, Alpacas, Turquoise Tealios, Gateron Black Inks, Kailh Box Navies, Buckling Spring, SKCM Blue Alps, Topre 45g, and NovelKeys Creams.
- **Virtual Gamepad**:
  - Dual 16-bit analog thumbsticks ($X/Y$ and $Z/R_x$), 8-direction hat switch D-pad, analog triggers ($R_y/R_z$), and 16 digital buttons.
  - Switchable console layout themes: **Xbox Series** (A/B/X/Y, View/Menu, Xbox Guide) and **PlayStation 5** ($\triangle$/$\bigcirc$/$\times$/$\square$, Create/Options, PS Guide).
  - Interactive Layout Customizer: Drag and scale thumbsticks, D-pads, and buttons with per-element coordinate persistence.
  - Haptic vibration feedback on button activation.
- **Smart TV Remote**:
  - Circular navigation D-pad, OK/Confirm, Back, Home, Power, Volume +/−/Mute, Play/Pause, Next/Previous/Stop, and Google Assistant key using standard Consumer Control report codes.
- **Developer / Agent Remote Console**:
  - Dedicated controller optimized for terminal workflows and coding agents (e.g., Claude Code CLI).
  - One-tap Core Commands: Yes (`y`), Yes to All, No (`n`), `Ctrl+C`, Backspace, Enter.
  - Built-in slash command presets: `/clear`, `/compact`, `/model`, `/btw`.
  - Custom macro manager: create, edit, sort, and trigger repetitive shell sequences.

### 2. Low-Latency LAN Wireless Audio Streaming

Nexus streams low-latency PC system audio to one or multiple Android devices over local Wi-Fi.

- **Audio Engine**:
  - High-fidelity Opus codec operating at 48 kHz stereo.
  - Configurable low frame lengths (10 ms or 20 ms) and bitrates (64, 96, 128, 192 kbps).
  - Windows WASAPI Loopback system audio capture via miniaudio.
  - Native C++ Opus decoding on Android via NDK/CMake.
- **High-Precision Clock Synchronization & Multi-Device Playback**:
  - Nanosecond-accuracy sender timestamps embedded in audio packets (`NX01`).
  - NTP-style four-timestamp round-trip probe protocol (`NXTS`) calculating running offset and network jitter.
  - Adaptive jitter buffer and playback scheduler eliminating drift and audio stuttering.
  - Multi-receiver support: stream simultaneously to multiple phones/tablets.
  - Independent channel routing per receiver: `Stereo` (pass-through), `Left` (mono duplicated to L/R), or `Right` (mono duplicated to L/R), allowing two phones to act as dedicated split left/right desktop speakers.
  - Receiver-to-receiver sync calibration protocol (`NXAC`) for multi-speaker acoustic alignment.
- **Zero-Configuration Discovery & Security**:
  - Automatic LAN discovery and advertising via mDNS (`_nexus-audio._udp`).
  - Cryptographic nonces and device fingerprint authorization (`NXCR`).
  - Sender and receiver approval prompts with permanent whitelist options ("Remember device").
  - Instant session termination signals (`ConnBye`) preventing lingering feedback loops.
- **Background & Lock-Screen Playback**:
  - Foreground Android Service with MediaSession integration and sticky lifecycle.
  - Automatic reconnection manager when network interfaces switch.

### 3. Modern Design & User Experience

- **Android**:
  - Pure Jetpack Compose architecture following Material Design 3 and Material You Expressive standards.
  - Dynamic Monet color scheme extraction (Android 12+), explicit dark/light mode toggle, and custom palette selection.
  - Customizable floating bottom navigation bar (reorder/toggle tabs: Home, Audio Receiver, Agent, TV Remote, Settings).
  - Fully bilingual interface (English and Simplified Chinese).
- **Desktop**:
  - Modern desktop interface built with Vue 3, TypeScript, and Wails v2.
  - Real-time connection feedback monitors, bitrate selectors, channel matrix router, NTP diagnostics, and trusted device manager.

---

## Network & Wire Protocol

All audio and control communication runs over UDP:

| Port | Protocol | Description |
| :--- | :--- | :--- |
| **`40125`** | UDP | **Android Receiver Port**: Receives audio datagrams (`NX01`), connection requests (`NXCR`), time sync probes (`NXTS`), setting sync (`NXCS`), and peer calibration (`NXAC`). |
| **`40126`** | UDP | **Desktop Control Port**: Receives inbound connection requests and session control from Android receivers. |

### Datagram Packet Types

- **`NX01` (Audio Frame)**: 40-byte header containing codec type, sample rate (48 kHz), channel count, bitrate, session ID, sequence counter, payload length, frame duration (10/20 ms), flags (FEC/DTX), and nanosecond sender capture timestamp (`timestampNs`), followed by Opus-encoded bytes.
- **`NXCR` (Connection Control)**: Handshake datagrams for connection request (`Kind=1`), response (`Kind=2`), and immediate disconnection notice (`Kind=3`), carrying random nonces and 64-byte UTF-8 device identities.
- **`NXCT` (Receiver Feedback)**: 35-byte periodic report (session, highest sequence, received, lost, buffer queue depth, current bitrate, sync state 0–3, clock offset in ms, and round-trip time).
- **`NXCS` (Stream Settings)**: Propagates bitrate and frame size adjustments across peers.
- **`NXTS` (Time Sync)**: 40-byte NTP-style round-trip probe carrying $T_1, T_2, T_3$ timestamps to calculate monotonic clock drift between host and receiver.
- **`NXAC` (Peer Calibration)**: Android-to-Android coordinate datagrams (`REQUEST`, `ACCEPT`, `REJECT`, `COMMIT`, `COMPLETE`, `CANCEL`) for speaker-to-speaker acoustic alignment.
- **`NXHB` (Heartbeat)**: Keepalive ping/pong packets maintaining connection state during idle or silent periods.

---

## Repository Structure

```text
Nexus/
├── android/                   # Android client project (Kotlin, Jetpack Compose, C++/NDK)
│   ├── app/
│   │   ├── src/main/cpp/      # CMakeLists.txt and third-party libopus source tree
│   │   ├── src/main/java/     # Kotlin application source code
│   │   │   └── com/haoze/nexus/
│   │   │       ├── audio/     # UDP streaming, JitterBuffer, ClockSync, Nsd, JNI bindings
│   │   │       ├── bluetooth/ # Android BluetoothHidDevice service, descriptors, senders
│   │   │       ├── macro/     # Macro definitions and repository for CLI agents
│   │   │       ├── sound/     # Mechanical keyboard switch audio synthesizer
│   │   │       ├── ui/        # Compose UI (Touchpad, Keyboard, Gamepad, TV Remote, Home)
│   │   │       └── util/      # Haptic feedback and platform helpers
│   │   └── build.gradle.kts   # Module build configuration and APK versioned output copy tasks
│   ├── build_apk.bat          # Interactive Android compilation & ADB device installer
│   └── gradlew.bat            # Gradle wrapper executable
├── desktop/                   # Windows desktop streamer (Go, Wails v2, Vue 3)
│   ├── frontend/              # Vue 3 + TypeScript + Vite frontend source
│   ├── internal/
│   │   ├── capture/           # WASAPI loopback audio capture (malgo)
│   │   ├── codec/             # libopus CGO wrapper and encoder logic
│   │   ├── config/            # Local settings and trusted device identity store
│   │   ├── discovery/         # mDNS advertiser and browser (zeroconf)
│   │   ├── gateway/           # Desktop UDP control listener
│   │   ├── ntp/               # External NTP verification helper
│   │   ├── protocol/          # Packet encoding/decoding (NX01, NXCR, NXCT, NXTS, NXCS)
│   │   └── stream/            # UDP multi-client streaming and keepalive engine
│   ├── app.go                 # Wails application bindings
│   ├── build.bat              # Standalone Windows NSIS installer & exe packaging script
│   └── wails.json             # Wails project configuration
├── output/                    # Unified build destination for all generated APKs and EXEs
├── scripts/                   # Auxiliary repository maintenance scripts
└── build_all.bat              # One-click master script to build both Android and Windows targets
```

---

## Prerequisites & Requirements

### Android Client
- **JDK**: Java Development Kit 11 or higher.
- **Android SDK**: Compile SDK `37`, Target SDK `37`, Min SDK `28` (Android 9.0+).
- **Android NDK**: Version `27.0.12077973` (installed via Android SDK Manager).
- **CMake**: 3.22.1 or newer.
- **Hardware**: Android device with Bluetooth HID Device profile support (`BluetoothHidDevice`).

### Windows Desktop Streamer
- **Operating System**: Windows 10 / 11 (64-bit).
- **Go**: Version 1.22 or newer.
- **Node.js & npm**: Node.js 18+ and current npm.
- **Wails CLI v2**: Installed via `go install github.com/wailsapp/wails/v2/cmd/wails@latest`.
- **C/C++ Compiler**: MinGW-w64 (GCC) or MSVC with CGO support.
- **libopus & pkg-config**: Development headers and static library for Opus accessible by `pkg-config`.
- **NSIS** *(Optional, for installer generation)*: Nullsoft Scriptable Install System on `PATH`.

---

## Building from Source

### 1. One-Click Unified Build (All Platforms)

To build both the signed Android APK and the Windows desktop binaries into a single directory:

```powershell
# Run the root build script (interactive mode)
.\build_all.bat

# Or run non-interactively (ideal for CI / automated pipelines):
.\build_all.bat --no-pause
```

Once completed, all artifacts will be copied automatically to the `output/` directory:
- `Nexus-debug-v1.1.0.apk` (and `Nexus-debug.apk`): Android package signed with release keystore.
- `Nexus.exe`: Standalone portable Windows executable.
- `Nexus-amd64-installer.exe`: Windows desktop setup installer.

---

### 2. Standalone Android Client Build

You can open the `android/` directory directly in Android Studio, or compile from the terminal:

```powershell
cd android

# Option A: Interactive script with ADB wireless/USB install menu:
.\build_apk.bat

# Option B: Direct Gradle compilation:
.\gradlew.bat assembleDebug -PsignDebugWithRelease=true
```

---

### 3. Standalone Windows Desktop Build

```powershell
cd desktop

# Live development mode with hot reload:
wails dev -tags "nexus_opus nolibopusfile"

# Production installer build:
.\build.bat
```

---

## How to Use

### Bluetooth HID Peripherals
1. Launch **Nexus** on your Android device.
2. In the top connection card, tap **Connect** to select or pair with your host device (PC, Mac, Smart TV, Tablet).
3. If connecting to a PC/console for gaming, switch the profile from **Keyboard / Mouse** to **Gamepad** in Settings or on the Home screen to ensure the host enumerates a native game controller.
4. Open the **Touchpad**, **Keyboard**, **Gamepad**, or **TV Remote** tab to start controlling.
5. In the Gamepad view, tap **Edit Layout** to reposition and resize on-screen thumbsticks, buttons, and triggers to fit your grip.

### LAN Audio Streaming
1. Ensure both your Windows PC and Android device are connected to the same Wi-Fi / LAN network.
2. Launch `Nexus.exe` on your Windows PC.
3. Open the **Audio Receiver** tab on the Android app and start the receiver service.
4. The desktop application will automatically discover the phone via mDNS.
5. Click **Connect** on the desktop card corresponding to your phone. Approve the pairing prompt on your phone (select "Remember" to avoid prompts in the future).
6. To configure a split stereo setup:
   - Connect two Android phones to the desktop streamer.
   - On the desktop device list, switch one device's channel route to **Left** and the second to **Right**. Both phones will now act as synchronized independent stereo speakers.

### Terminal Agent Console
1. Navigate to the **Agent** tab on Android.
2. Pair via Bluetooth HID to your development workstation.
3. With a terminal running an AI coding assistant (such as Claude Code) focused on your workstation, use one-tap responses (**Yes**, **No**, **Yes to All**, **Ctrl+C**) or trigger custom slash commands without touching the physical keyboard.

---

## Recognition & Acknowledgments

We express sincere gratitude to all community supporters and co-builders who contribute to the project. See [`recognition_members.json`](./recognition_members.json) for the full list of recognized contributors and sponsors.
