# Nexus 项目版本号存储与修改指南

本指南记录了 **Nexus** 项目中版本号的存储位置、字段作用及版本更新时的修改规范。

---

## 一、版本号存储文件清单

Nexus 采用“**Android 移动客户端 + Windows 桌面端 (Go & Wails + Vue3) + 统一构建输出**”的架构。修改软件版本时，需要同步更新以下文件中的版本信息：

| 模块 / 平台 | 相对路径 | 涉及字段 | 说明 |
| :--- | :--- | :--- | :--- |
| **Android 客户端** | `android/app/build.gradle.kts` | `versionName`<br>`versionCode` | 客户端核心版本配置，供应用自身读取及应用商店识别 |
| **Windows 桌面宿主** | `desktop/wails.json` | `info.productVersion` | Wails 桌面打包配置，注入到 Windows exe 属性及安装包 |
| **桌面前端工程** | `desktop/frontend/package.json` | `version` | 前端 Vue3 模块版本标识 |
| **桌面前端锁文件** | `desktop/frontend/package-lock.json` | `version` (根包及项目) | npm 依赖锁定文件，与 `package.json` 保持同步 |
| **项目主说明文档** | `README.md` | 构建产物命名示例 | 记录打包输出的 APK 文件名规范 |

---

## 二、各文件详细配置与修改方法

### 1. Android 客户端：`android/app/build.gradle.kts`

* **相对路径**：`android/app/build.gradle.kts`
* **配置区块**：
  ```kotlin
  android {
      ...
      defaultConfig {
          applicationId = "com.haoze.nexus"
          minSdk = 28
          targetSdk = 37
          versionCode = 5        // 内部版本号（递增整数）
          versionName = "1.1.0"  // 对外显示版本号（语义化字符串）

          testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      }
      ...
  }
  ```
* **字段说明与规范**：
  * **`versionName`**：面向用户的可见版本号（如 `"1.1.0"`）。Android 端内的设置页面、关于页面以及生成的 APK 文件名均动态引用该字段。
  * **`versionCode`**：Android 系统和应用市场用来判断版本先后顺序的内部整数。**每次发布新版本时必须比前一版本加 1**（例如从 `4` 增加到 `5`），否则设备上直接覆盖安装时会被系统拦截报错。

---

### 2. Windows 桌面宿主：`desktop/wails.json`

* **相对路径**：`desktop/wails.json`
* **配置区块**：
  ```json
  {
    "$schema": "https://wails.io/schemas/config.v2.json",
    "name": "Nexus",
    "outputfilename": "Nexus",
    "info": {
      "productVersion": "1.1.0"
    },
    "frontend:dir": "frontend",
    "frontend:install": "npm install",
    "frontend:build": "npm run build",
    "frontend:dev:watcher": "npm run dev"
  }
  ```
* **字段说明与规范**：
  * **`info.productVersion`**：桌面端产品版本。Wails 构建 Windows 安装包与单文件时，会依据此字段填充 Windows PE 可执行文件的属性信息（右键属性 -> 详细信息 -> 产品版本）。

---

### 3. 桌面前端工程：`desktop/frontend/package.json` 与 `package-lock.json`

* **相对路径**：
  * `desktop/frontend/package.json`
  * `desktop/frontend/package-lock.json`
* **配置区块** (`desktop/frontend/package.json`)：
  ```json
  {
    "name": "nexus-frontend",
    "private": true,
    "version": "1.1.0",
    "type": "module",
    ...
  }
  ```
* **配置区块** (`desktop/frontend/package-lock.json`)：
  ```json
  {
    "name": "nexus-frontend",
    "version": "1.1.0",
    "lockfileVersion": 3,
    "requires": true,
    "packages": {
      "": {
        "name": "nexus-frontend",
        "version": "1.1.0",
        ...
      }
    }
  }
  ```
* **修改说明**：
  * 可直接编辑上述两个文件中的 `"version"` 字段，或在终端进入 `desktop/frontend` 目录执行 `npm version 1.1.0 --no-git-tag-version` 自动完成两个文件的同步更新。

---

### 4. 项目主文档：`README.md`

* **相对路径**：`README.md`
* **涉及内容**：在“构建产物说明”章节中，记录了带有版本号的安装包命名规则：
  ```markdown
  Once completed, all artifacts will be copied automatically to the `output/` directory:
  - `Nexus-debug-v1.1.0.apk` (and `Nexus-debug.apk`): Android package signed with release keystore.
  - `Nexus.exe`: Standalone portable Windows executable.
  - `Nexus-amd64-installer.exe`: Windows desktop setup installer.
  ```
* **修改说明**：同步将构建输出中的 `Nexus-debug-v<版本号>.apk` 修改为当前新版本号。

---

## 三、版本号修改后的验证与打包

修改完成后，可通过以下步骤验证构建与版本产物：

1. **一键全平台构建**：
   在项目根目录下运行：
   ```powershell
   .\build_all.bat --no-pause
   ```
2. **检查输出目录**：
   检查根目录下的 `output/` 文件夹，确认生成的文件名包含新版本号：
   * `output/Nexus-debug-v1.1.0.apk`
   * `output/Nexus.exe`
   * `output/Nexus-amd64-installer.exe`
