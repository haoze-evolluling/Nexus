# Android 签名证书管理与发布签名规范

本文档用于记录 Nexus 项目全新 Android 正式发布签名证书的技术规格、安全管理规范、离线私有保管流程以及多环境配置指引。

---

## 1. 迁移背景与旧证书废弃说明

在早期开发阶段，项目仓库中曾直接包含了 `app/keystore/debug.keystore`，且在 `app/build.gradle.kts` 中对 Release 构建也指定了该调试证书及明文密码（`android`）。

这种做法存在严重的**密钥暴露安全隐患**：
1. **私钥泄露**：任何获取代码仓库的人都可以提取私钥并伪造合法签名的恶意更新包。
2. **身份不可信**：调试证书的所有人与颁发者均为通用的 `Android Debug`，无法确立开发团队的真实软件发布者身份。

**迁移处置：**
- **建立全新签名体系**：重新生成高强度（RSA 4096 位、PKCS12 格式）的专属正式签名证书，今后统一使用新证书编译和签名发布版本。
- **凭据与代码解耦**：密钥文件和凭据严格置于本地（`Nexus-keystore/` 或 `SyncTouch-keystore/`），仅提交脱敏模版 `keystore.properties.example`，避免敏感凭据再次入库。
- **Release 构建安全**：Release 编译默认加载正式签名证书，支持 V1/V2/V3 全套签名方案；在缺少本地密钥配置时自动跳过签名，杜绝调试证书硬编码漏洞。

---

## 2. 新证书技术规格与指纹

新生成的正式发布证书技术参数如下：

| 参数项 | 取值 |
| :--- | :--- |
| **密钥库文件** | `Nexus-keystore/nexus-release.jks`（或 `SyncTouch-keystore/synctouch-release.jks`，本地私有，不入库） |
| **密钥库格式** | `PKCS12`（业界标准格式） |
| **签名别名 (Alias)** | `nexus-release`（或 `synctouch-release`） |
| **密钥算法与长度** | `RSA` / `4096 bits` |
| **签名算法** | `SHA384withRSA` |
| **证书所有人 (DName)** | `CN=Nexus, OU=Nexus Team, O=Nexus, C=CN` |
| **证书序列号** | `b6eeee934f15f91f` |
| **有效期至** | 2054 年 02 月 10 日（10,000 天） |

### 证书指纹 (Certificate Fingerprints)
在发布或验证第三方平台集成时，可使用以下指纹进行核对：

- **SHA-256**:
  ```text
  15:7C:90:8A:7C:5E:37:A4:4E:9F:FF:AB:EF:00:B5:CF:53:97:37:9B:B4:AA:94:A3:ED:01:8B:52:35:F8:F7:AE
  ```
- **SHA-1**:
  ```text
  10:50:D6:0A:7E:1F:30:D3:C4:3D:13:61:6D:BC:A2:1D:AF:C4:2F:C8
  ```

---

## 3. 私密凭据保管规范（Google Drive 离线/私有存储）

> [!CAUTION]
> **绝对禁止将 `.jks` 证书文件及包含真实密码的 `keystore.properties` 提交到 GitHub 仓库或任何公开网络介质！**
> `.gitignore` 已配置规则拦截相关文件，但仍需保持安全意识。

### Google Drive 归档指引
新证书及密钥凭据仅保存在个人私有 Google Drive 中进行离线/私密保管：

1. **创建专用保管目录**：
   在个人 Google Drive 根目录下创建专用私密文件夹，如：
   `My Drive/Private-Credentials/Nexus-Android-Signing/`
2. **上传必要凭据（推荐直接上传整个 `Nexus-keystore` 文件夹）**：
   本地所有的签名私密文件均统一存放在 `Nexus-keystore/` 目录下（受 `.gitignore` 保护不入库）：
   - `Nexus-keystore/nexus-release.jks`（密钥库文件）
   - `Nexus-keystore/keystore.properties`（签名密码与别名配置）
   您可以直接将整个 `Nexus-keystore` 目录（或压缩包）上传至 Google Drive 专用目录中。
3. **安全权限控制**：
   - 确保该文件夹的共享设置处于 **“仅限个人访问”（Restricted）**，切勿创建公开可访问链接。
   - 确保个人 Google 账号已启用两步验证（2FA / Passkey）。
4. **冗余冷备份（推荐）**：
   将上述凭据在个人加密 U 盘或离线固态硬盘中存放一份脱机冷备份，防止单个云服务不可用风险。

---

## 4. 多设备开发与构建配置流程

当在新开发机、重装系统后或授权开发者需要编译正式签名包时，请按如下步骤配置：

### 步骤 1：获取密钥
1. 从个人的私有 Google Drive 目录中下载 `Nexus-keystore` 文件夹（包含密钥文件和 `keystore.properties`）。
2. 放置到项目根目录下：
   ```text
   Nexus/
   └── Nexus-keystore/
       ├── nexus-release.jks
       └── keystore.properties
   ```
   > 注：若之前仅备份了密钥文件与密码记录，也可复制根目录下的 `keystore.properties.example` 至 `Nexus-keystore/keystore.properties`，填入实际密码即可。项目同时兼容读取 `SyncTouch-keystore/keystore.properties`。

### 步骤 2：编译与打包构建选项
- **使用构建脚本（推荐）**：
   直接运行项目根目录下的 `build_apk.bat`，菜单提供以下选项：
  - `[1] Debug`：开发调试版，使用本地 Android SDK 默认调试密钥。
  - `[2] Release`：正式发布包，开启 R8 混淆优化，使用 `Nexus-keystore` 正式证书签名。
  - `[3] Debug (Release Key)`：开发调试版，快速编译且可断点调试，但统一使用 `Nexus-keystore` 正式证书签名（支持直接覆盖安装测试，无需卸载已有的正式版）。
  - `[4] Clean Project`：一键执行 `./gradlew clean` 清理历史构建缓存。
- **使用命令行**：
  ```powershell
  # 编译正式发布版
  .\gradlew.bat assembleRelease

  # 编译使用 Release 证书签名的 Debug 版（方便覆盖安装）
  .\gradlew.bat assembleDebug -PsignDebugWithRelease=true

  # 清理项目构建缓存
  .\gradlew.bat clean
  ```
- **构建产物位置**：
  - 标准输出路径：`app/build/outputs/apk/release/app-release.apk`
  - 带版本号命名输出：`app/build/outputs/apk/versioned/release/Nexus-release-v<version>.apk`

---

## 5. 签名验证操作参考

在发布正式 APK 前，可随时通过 Android SDK 的 `apksigner` 工具检验 APK 的签名完整性与签名方案生效情况：

```powershell
# 验证签名并打印证书信息
& "D:\Androidsdk\build-tools\37.0.0\apksigner.bat" verify --verbose --print-certs "app/build/outputs/apk/release/app-release.apk"
```

输出应满足：
- `Verifies` 为成功状态。
- `Verified using v3 scheme (APK Signature Scheme v3): true`。
- 证书 SHA-256 指纹与本规范第 2 节记录的指纹完全相符。
