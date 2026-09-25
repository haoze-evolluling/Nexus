# Nexus

Nexus 是一款集**低延迟蓝牙外设模拟**与**局域网高保真无线音频串流**于一体的全能跨端控制套件。

- **移动端（Android）**：提供基于标准蓝牙 HID 的虚拟触控板、机械按键音效键盘、双摇杆游戏手柄、电视遥控器，并内置超低延迟 Opus 音频串流接收引擎与微秒级多设备时钟同播对齐。
- **桌面端（Windows）**：基于 Wails v2 + Vue 3 构建的轻量级发送端，通过 WASAPI Loopback 实时捕获系统音频并经 Opus 高保真压缩，毫秒级推流至一台或多台 Android 设备。

---

## ✨ 核心特性

### 1. 局域网高保真无线音频串流
- **超低延迟传输**：基于 48 kHz 双声道 Opus 实时编解码，支持 10 ms / 20 ms 极低帧长及 64 ~ 192 kbps 动态码率。
- **高精度时钟同步与平滑播放**：NTP 风格纳秒级往返时钟对齐结合 Jitter Buffer 与自适应微调重采样，彻底消除跳音与多设备同播回音。
- **分体式多声道与同播校准**：单台电脑可向多台 Android 设备分发独立声道（立体声 / 左 / 右），支持设备间直接发起同播校准（NXAC）。
- **双向 mDNS 发现与授权**：两端自动发现并支持双向发起连接，提供设备指纹授权与免密重连。

### 2. 全功能蓝牙 HID 外设模拟
- **虚拟触控板**：精准手势滑动、双指滚动、左右键点按与平滑指针加速。
- **模拟键盘与按键音效**：全键盘布局，内置 Alpaca、Black Ink、Blue Alps 等多种经典机械键盘按键原声合成音效。
- **游戏手柄与触感反馈**：双模拟摇杆、方向十字键与功能按键，配备操作振动触感反馈。
- **电视遥控与快捷指令**：便捷控制智能电视 / PC 多媒体播放，支持自定义宏命令。

### 3. 现代设计与极佳视觉
- **Material 3 & Monet**：全面支持 Android 12+ 动态取色、深浅色模式与流体交互质感。

---

## 📦 项目架构

```text
Nexus/
├── android/              # Android 客户端工程（Kotlin、Jetpack Compose、NDK/CMake）
│   ├── app/              # 主模块源码及 CMakeLists.txt
│   ├── gradle/           # Gradle Wrapper 及依赖版本管理
│   ├── Nexus-keystore/   # 签名密钥库配置
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew.bat
│   └── build_apk.bat     # Android 编译与调试安装脚本
├── desktop/              # Windows 桌面发送端工程（Go、Wails v2、Vue 3）
│   ├── frontend/         # Vue 3 用户界面
│   ├── internal/         # WASAPI 采集、Opus 编码、mDNS 发现、UDP 传输引擎
│   ├── wails.json
│   └── build.bat         # Windows 安装包与可执行文件打包脚本
├── output/               # 统一构建产物调试输出目录（两端 APK 与 EXE/Installer 汇集地）
├── build_all.bat         # 跨端一键构建脚本（打包两端并统一同步输出到 output/）
├── build_apk.bat         # 根目录便捷打包脚本（自动转调 android/build_apk.bat）
└── recognition_members.json
```

---

## 🛠️ 构建与开发

### 一键构建全工程（Windows）

```powershell
# 编译 Android Debug APK（使用 Release 签名证书）并构建 Windows 桌面端，产物统一输出至 output/
.\build_all.bat

# CI / 脚本静默调用（构建完成后不暂停）：
.\build_all.bat --no-pause
```

> **构建产物统一目录**：构建成功后，所有可执行文件与安装包将自动汇总至根目录的 `output/` 文件夹中：
> - `Nexus-debug-vX.X.X.apk` / `Nexus-debug.apk`：已签名 Release 证书的 Android 端安装包。
> - `Nexus.exe`：免安装绿色版桌面端可执行程序，双击即可进行调试。
> - `Nexus-amd64-installer.exe`：Windows 桌面端完整安装包。

### Android 客户端

- **环境要求**：JDK 11+、Android SDK（Compile SDK 37，Min SDK 28）、Android NDK（`27.0.12077973`）、CMake。
- **Android Studio**：直接打开 `Nexus/android` 目录即可开始开发调试。
- **独立编译**：
  ```powershell
  cd android
  .\gradlew.bat assembleDebug -PsignDebugWithRelease=true
  ```
  或者在根目录直接运行快捷打包脚本：
  ```powershell
  .\build_apk.bat
  ```

### Windows 桌面发送端

- **环境要求**：Go 1.26+、Node.js、Wails CLI、C/C++ 编译器（MSVC 或 MinGW-w64）、libopus。
- **编译运行**：
  ```powershell
  cd desktop
  wails dev -tags "nexus_opus nolibopusfile"
  ```
- **独立打包**：
  ```powershell
  cd desktop
  .\build.bat
  ```
