import { computed, ref } from 'vue';

export type Locale = 'zh' | 'en';
export type LanguagePref = 'system' | 'zh' | 'en';
export type ParamValue = string | number;

const languageKey = 'steamvoice.desktop.language';
const svmsgPrefix = 'svmsg:';

/** 会话进行中由 Go 后端报告的“正常传输”状态码，前端据此区分正常与告警。 */
export const STREAMING_CODE = 'svmsg:streaming';

const messages: Record<Locale, Record<string, string>> = {
  zh: {
    'header.eyebrow': '电脑音频发送端',
    'header.back': '返回',
    'header.settings': '设置',
    'header.single': '已连接 {n} 台接收端，正在发送电脑音频',
    'header.multi': '已连接 {n} 台接收端 · 同步播放中',
    'status.idle': '未连接接收端',
    'status.discoverFailed': '无法发现接收端',
    'status.allowed': '已允许 {name} 连接',
    'status.respondFailed': '授权失败：{detail}',
    'status.removeFailed': '移除授权失败',
    'status.frameUnsupported': '该接收端不支持 {frame} ms 音频帧',
    'status.waitingConfirm': '正在等待 {name} 确认连接请求…',
    'status.connectFailed': '连接失败：{detail}',
    'status.disconnectFailed': '断开连接失败',
    'status.getStatusFailed': '无法获取连接状态',
    'devices.title': '可用接收端',
    'devices.hint': '同一局域网内已启动接收服务的设备，可同时连接多台外放',
    'devices.rescan': '重新扫描',
    'device.supports': '支持 {frames} ms',
    'device.connect': '连接',
    'device.disconnect': '断开',
    'device.waiting': '等待确认…',
    'empty.scanning': '正在扫描局域网接收端…',
    'empty.hint': '请确认手机端 SyncTouch 已打开并连接到同一 Wi-Fi',
    'sync.doneTitle': '多设备同步完成',
    'sync.doneHint': '各设备已对齐统一时间基准，回到正常播放',
    'sync.busyTitle': '正在同步校准多台设备',
    'sync.busyHint': '正在对齐各设备的播放时钟，校准过程不会产生跳音',
    'sync.summaryItem': '{name} 偏差 {offset} ms',
    'sync.note': '♪ 多台接收端已按统一时间基准同步播放，自动校准时不会产生跳音',
    'calib.stats': '偏差 {offset} ms · 往返 {rtt} ms',
    'phase.detect': '检测',
    'phase.calculate': '计算',
    'phase.sync': '同步',
    'phase.done': '完成',
    'phaseHint.detect': '检测接收信号',
    'phaseHint.calculate': '测量时钟偏差',
    'phaseHint.sync': '对齐播放时刻',
    'phaseHint.done': '同步播放中',
    'settings.hint': '音频参数会在下一次连接时生效',
    'settings.bitrate': '初始 Opus 码率',
    'settings.frame': '音频帧时长',
    'settings.frameWarning': '当前已连接的接收端不支持所选帧时长；请先断开全部设备后再更改。',
    'settings.ntp': 'NTP 时间校准', 'settings.ntpHint': '默认 ntp.aliyun.com；不可用时不影响局域网播放同步。', 'settings.ntpDefault': '恢复默认', 'settings.ntpTest': '测试服务器', 'settings.ntpSaved': '服务器已保存', 'settings.ntpInvalid': '服务器地址无效', 'settings.ntpUnavailable': '无法连接 NTP 服务器', 'settings.ntpOffset': '本机时间偏移 {offset} ms',
    'settings.appearance': '外观',
    'settings.language': '语言',
    'settings.authorized': '已授权设备',
    'settings.authorizedHint': '以下设备连接这台电脑时无需再次确认',
    'settings.authorizedEmpty': '暂无已授权设备。当 Android 设备主动连接时，可以选择记住它。',
    'settings.localInfo': '本机信息',
    'theme.light': '浅色',
    'theme.dark': '深色',
    'theme.system': '跟随系统',
    'language.system': '跟随系统',
    'language.zh': '简体中文',
    'language.en': 'English',
    'device.unnamed': '未命名设备',
    'device.remove': '移除',
    'info.name': '设备名称：{value}',
    'info.unnamed': '未命名',
    'info.id': '设备标识：{value}',
    'info.codec': '编码器：Opus',
    'info.sampleRate': '采样率：48 kHz',
    'info.channels': '声道：立体声',
    'footer.connectedPrefix': '已连接 {n} 台 · ',
    'footer.tail': '48 kHz 立体声 Opus · UDP 局域网传输',
    'modal.title': '连接请求',
    'modal.morePending': '（还有 {n} 个待处理）',
    'modal.desc': '想要把这台电脑的音频推送到它上面播放。',
    'modal.remember': '以后自动同意该设备',
    'modal.deny': '拒绝',
    'modal.allow': '允许',
    'modal.unknownDevice': '未知设备',
    // Go 后端消息码（svmsg:<code>）的翻译。
    'backend.streaming': '正在传输系统音频',
    'backend.disconnected': '已断开连接',
    'backend.idle': '未连接接收端',
    'backend.connected': '已连接 {p} 台接收端，正在发送电脑音频',
    'backend.receiver_unresponsive': '接收端无响应',
    'backend.receiver_dropped': '接收端长时间无响应，已断开',
    'backend.err_bitrate': '不支持的码率：{p} kbps',
    'backend.err_frame': '不支持的音频帧时长：{p} ms',
    'backend.err_frame_receiver': '接收端不支持 {p} ms 音频帧',
    'backend.err_codec': '接收端不支持 Opus（codec={p}）',
    'backend.err_frame_in_use': '已连接的接收端正在使用 {p} ms 音频帧，请先断开全部设备再更改帧时长',
    'backend.err_denied': '接收端未同意连接（对方拒绝了请求、长时间未确认或版本过旧）',
    'backend.err_opus_init': '初始化 Opus 编码器失败（{p}）',
    'backend.err_capture': '启动 WASAPI 系统音频采集失败（{p}）',
    'backend.err_request_expired': '连接请求已过期或已处理',
    'backend.err_control': '控制通道不可用',
    'backend.err_respond': '发送授权结果失败（{p}）',
    'backend.err_connect': '连接失败：{p}',
    'backend.err_channel_route': '无效的声道路由：{p}',
    'backend.err_not_connected': '设备未连接',
    'channel.label': '声道路由',
    'channel.stereo': '立体声',
    'channel.left': '左声道',
    'channel.right': '右声道',
  },
  en: {
    'header.eyebrow': 'PC audio sender',
    'header.back': 'Back',
    'header.settings': 'Settings',
    'header.single': 'Connected to {n} receiver, streaming PC audio',
    'header.multi': 'Connected to {n} receivers · playing in sync',
    'status.idle': 'No receiver connected',
    'status.discoverFailed': 'Could not discover receivers',
    'status.allowed': 'Allowed {name} to connect',
    'status.respondFailed': 'Authorization failed: {detail}',
    'status.removeFailed': 'Failed to remove the authorization',
    'status.frameUnsupported': 'This receiver does not support {frame} ms audio frames',
    'status.waitingConfirm': 'Waiting for {name} to accept the connection request…',
    'status.connectFailed': 'Connection failed: {detail}',
    'status.disconnectFailed': 'Failed to disconnect',
    'status.getStatusFailed': 'Could not get the connection status',
    'devices.title': 'Available receivers',
    'devices.hint': 'Devices on the same LAN running the receiver service; connect several at once',
    'devices.rescan': 'Rescan',
    'device.supports': 'Supports {frames} ms',
    'device.connect': 'Connect',
    'device.disconnect': 'Disconnect',
    'device.waiting': 'Waiting…',
    'empty.scanning': 'Scanning the LAN for receivers…',
    'empty.hint': 'Make sure SyncTouch is open on the phone and both are on the same Wi-Fi',
    'sync.doneTitle': 'Multi-device sync complete',
    'sync.doneHint': 'All devices are aligned to a shared timebase, back to normal playback',
    'sync.busyTitle': 'Calibrating multi-device sync',
    'sync.busyHint': 'Aligning the playback clocks of every device; calibration causes no skips',
    'sync.summaryItem': '{name} offset {offset} ms',
    'sync.note': '♪ Receivers are playing in sync on a shared timebase; auto-calibration causes no skips',
    'calib.stats': 'offset {offset} ms · RTT {rtt} ms',
    'phase.detect': 'Detect',
    'phase.calculate': 'Calculate',
    'phase.sync': 'Sync',
    'phase.done': 'Done',
    'phaseHint.detect': 'Detecting the receiver signal',
    'phaseHint.calculate': 'Measuring clock offset',
    'phaseHint.sync': 'Aligning playback time',
    'phaseHint.done': 'Playing in sync',
    'settings.hint': 'Audio settings take effect on the next connection',
    'settings.bitrate': 'Initial Opus bitrate',
    'settings.frame': 'Audio frame duration',
    'settings.frameWarning': 'A connected receiver does not support the selected frame duration; disconnect all devices before changing it.',
    'settings.ntp': 'NTP time calibration', 'settings.ntpHint': 'Defaults to ntp.aliyun.com; LAN playback sync continues if it is unavailable.', 'settings.ntpDefault': 'Restore default', 'settings.ntpTest': 'Test server', 'settings.ntpSaved': 'Server saved', 'settings.ntpInvalid': 'Invalid server address', 'settings.ntpUnavailable': 'Could not reach the NTP server', 'settings.ntpOffset': 'Local clock offset {offset} ms',
    'settings.appearance': 'Appearance',
    'settings.language': 'Language',
    'settings.authorized': 'Authorized devices',
    'settings.authorizedHint': 'These devices can connect to this PC without confirmation',
    'settings.authorizedEmpty': 'No authorized devices yet. When an Android device connects, you can choose to remember it.',
    'settings.localInfo': 'This PC',
    'theme.light': 'Light',
    'theme.dark': 'Dark',
    'theme.system': 'Follow system',
    'language.system': 'Follow system',
    'language.zh': '简体中文',
    'language.en': 'English',
    'device.unnamed': 'Unnamed device',
    'device.remove': 'Remove',
    'info.name': 'Device name: {value}',
    'info.unnamed': 'Unnamed',
    'info.id': 'Device ID: {value}',
    'info.codec': 'Codec: Opus',
    'info.sampleRate': 'Sample rate: 48 kHz',
    'info.channels': 'Channels: stereo',
    'footer.connectedPrefix': 'Connected: {n} · ',
    'footer.tail': '48 kHz stereo Opus · UDP over LAN',
    'modal.title': 'Connection request',
    'modal.morePending': '({n} more pending)',
    'modal.desc': 'wants to stream this PC’s audio to itself.',
    'modal.remember': 'Always allow this device',
    'modal.deny': 'Deny',
    'modal.allow': 'Allow',
    'modal.unknownDevice': 'Unknown device',
    // Translations for Go backend message codes (svmsg:<code>).
    'backend.streaming': 'Streaming system audio',
    'backend.disconnected': 'Disconnected',
    'backend.idle': 'No receiver connected',
    'backend.connected': 'Connected to {p} receiver(s), streaming PC audio',
    'backend.receiver_unresponsive': 'Receiver not responding',
    'backend.receiver_dropped': 'Receiver unresponsive for too long, disconnected',
    'backend.err_bitrate': 'Unsupported bitrate: {p} kbps',
    'backend.err_frame': 'Unsupported frame duration: {p} ms',
    'backend.err_frame_receiver': 'The receiver does not support {p} ms audio frames',
    'backend.err_codec': 'The receiver does not support Opus (codec={p})',
    'backend.err_frame_in_use': 'Connected receivers are using {p} ms audio frames; disconnect all of them before changing the frame duration',
    'backend.err_denied': 'The receiver did not accept the connection (rejected, left unconfirmed, or running an older version)',
    'backend.err_opus_init': 'Failed to initialize the Opus encoder ({p})',
    'backend.err_capture': 'Failed to start WASAPI system audio capture ({p})',
    'backend.err_request_expired': 'The connection request expired or was already handled',
    'backend.err_control': 'Control channel unavailable',
    'backend.err_respond': 'Failed to send the authorization result ({p})',
    'backend.err_connect': 'Connection failed: {p}',
    'backend.err_channel_route': 'Invalid channel route: {p}',
    'backend.err_not_connected': 'Device not connected',
    'channel.label': 'Channel route',
    'channel.stereo': 'Stereo',
    'channel.left': 'Left',
    'channel.right': 'Right',
  },
};

function systemLocale(): Locale {
  return (navigator.language || 'zh').toLowerCase().startsWith('zh') ? 'zh' : 'en';
}

const stored = localStorage.getItem(languageKey);
export const language = ref<LanguagePref>(stored === 'zh' || stored === 'en' ? stored : 'system');

export const locale = computed<Locale>(() => (language.value === 'system' ? systemLocale() : language.value));

export function setLanguage(pref: LanguagePref) {
  language.value = pref;
  localStorage.setItem(languageKey, pref);
  document.documentElement.lang = locale.value === 'zh' ? 'zh-CN' : 'en';
}

export function t(key: string, params?: Record<string, ParamValue>): string {
  const table = messages[locale.value] ?? messages.zh;
  let text = table[key] ?? messages.zh[key] ?? key;
  if (params) for (const [name, value] of Object.entries(params)) text = text.replaceAll(`{${name}}`, String(value));
  return text;
}

/** 解析 Go 后端的 svmsg:<code>[:<detail>] 消息码并按当前语言翻译；detail 可嵌套另一个码。 */
export function translateBackend(raw: string): string {
  if (!raw.startsWith(svmsgPrefix)) return raw;
  const body = raw.slice(svmsgPrefix.length);
  const colon = body.indexOf(':');
  const code = colon < 0 ? body : body.slice(0, colon);
  const detail = colon < 0 ? '' : body.slice(colon + 1);
  const param = detail ? translateBackend(detail) : '';
  return t(`backend.${code}`, param ? { p: param } : undefined);
}
