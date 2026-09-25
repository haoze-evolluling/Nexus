<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { Connect, DiscoverDevices, Disconnect, GetStatus, GetIdentity, GetNTPSettings, ListAuthorizedDevices, RemoveAuthorizedDevice, RespondConnection, SaveLocalSettings, SaveNTPServer, SetChannelRoute, TestNTPServer } from '../wailsjs/go/main/App';
import { EventsOn, WindowSetDarkTheme, WindowSetLightTheme, WindowSetSystemDefaultTheme } from '../wailsjs/runtime/runtime';
import { STREAMING_CODE, language, locale, setLanguage, t, translateBackend, type LanguagePref, type ParamValue } from './i18n';

type Theme = 'light' | 'dark' | 'system';
type Settings = { bitrate: number; frameMs: number; theme: Theme; updatedAtMs: number; deviceId: string };
type Device = { name: string; host: string; port: number; id: string; codec: string; sampleRate: number; channels: number; bitrate: number; frameMs: number; supportedFrameMs: number[]; updatedAtMs: number; settingsDeviceId: string };
type ChannelRoute = 'stereo' | 'left' | 'right';
type DeviceStatus = { deviceId: string; name: string; connected: boolean; message: string; bitrate: number; frameMs: number; phase: number; channelRoute: ChannelRoute };
type ConnRequest = { requestId: string; deviceId: string; name: string; host: string };
type AuthorizedDevice = { ID: string; Name: string; AddedAtMs: number };
type Calibration = { phase: number; offsetMs: number; rttMs: number; updatedAt: number };
const phaseLabels = computed(() => [t('phase.detect'), t('phase.calculate'), t('phase.sync'), t('phase.done')]);
const phaseHints = computed(() => [t('phaseHint.detect'), t('phaseHint.calculate'), t('phaseHint.sync'), t('phaseHint.done')]);
const languageOptions: LanguagePref[] = ['system', 'zh', 'en'];
const settingsKey = 'nexus.desktop.settings.v2';
const deviceIdKey = 'nexus.desktop.device_id';
const deviceId = localStorage.getItem(deviceIdKey) ?? crypto.randomUUID();
localStorage.setItem(deviceIdKey, deviceId);
const defaults: Settings = { bitrate: 128000, frameMs: 10, theme: 'system', updatedAtMs: 0, deviceId };
const settings = ref<Settings>({ ...defaults });
const themeOptions: Theme[] = ['light', 'dark', 'system'];
const page = ref<'devices' | 'settings'>('devices');
const devices = ref<Device[]>([]);
// 状态条文案统一在展示时翻译：key 为前端文案键，backend 为 Go 的 nxmsg 码，
// 语言切换后无需等待下一条消息即自动切换显示语言。
type StatusInfo = { kind: 'key' | 'backend'; value: string; params?: Record<string, ParamValue> } | null;
const statusInfo = ref<StatusInfo>(null);
const status = computed(() => {
  const info = statusInfo.value;
  if (!info) return t('status.idle');
  return info.kind === 'key' ? t(info.value, info.params) : translateBackend(info.value);
});
function setStatus(key: string, params?: Record<string, ParamValue>) { statusInfo.value = { kind: 'key', value: key, params }; }
function setStatusBackend(raw: string) { statusInfo.value = { kind: 'backend', value: raw }; }
function setErrorStatus(prefixKey: string, error: unknown) {
  const message = error instanceof Error ? error.message : String(error);
  if (message.startsWith('nxmsg:') || message.startsWith('svmsg:')) setStatusBackend(message);
  else setStatus(prefixKey, { detail: message });
}
const connected = ref<Record<string, DeviceStatus>>({});
const connRequests = ref<ConnRequest[]>([]);
const rememberChoice = ref(true);
const identity = ref<{ deviceId: string; name: string }>({ deviceId: '', name: '' });
const authorizedDevices = ref<AuthorizedDevice[]>([]);
const connecting = ref<Record<string, boolean>>({});
const calibration = ref<Record<string, Calibration>>({});
const ntpServer = ref('ntp.aliyun.com');
const ntpResult = ref<string>('');
const syncFlash = ref(false);
const connectedCount = computed(() => Object.keys(connected.value).length);
const connectedDevices = computed(() => devices.value.filter((device) => connected.value[device.id]));
const headerStatus = computed(() => (connectedCount.value > 0 ? t(connectedCount.value > 1 ? 'header.multi' : 'header.single', { n: connectedCount.value }) : status.value));
const supportedFrames = computed(() => {
  if (connectedDevices.value.length) return connectedDevices.value.reduce((acc: number[], device) => acc.filter((frame) => device.supportedFrameMs.includes(frame)), [10, 20]);
  return devices.value.length ? Array.from(new Set(devices.value.flatMap((device) => device.supportedFrameMs))) : [10, 20];
});
const frameAvailable = computed(() => connectedDevices.value.every((device) => device.supportedFrameMs.includes(settings.value.frameMs)));

function normalizeSettings(raw: unknown): Settings {
  const value = raw as Partial<Settings> | null;
  return { bitrate: [64000, 96000, 128000, 192000].includes(value?.bitrate ?? 0) ? value!.bitrate! : defaults.bitrate, frameMs: value?.frameMs === 20 ? 20 : 10, theme: value?.theme === 'light' || value?.theme === 'dark' || value?.theme === 'system' ? value.theme : defaults.theme, updatedAtMs: Number(value?.updatedAtMs) || 0, deviceId: typeof value?.deviceId === 'string' && value.deviceId ? value.deviceId : deviceId };
}
function normalizeDevice(raw: any): Device {
  const values = raw?.supportedFrameMs ?? raw?.SupportedFrameMs ?? [raw?.frameMs ?? raw?.FrameMs ?? 10];
  const supportedFrameMs = (Array.isArray(values) ? values : [values]).filter((value) => value === 10 || value === 20);
  return { name: raw?.name ?? raw?.Name ?? 'Android device', host: raw?.host ?? raw?.Host ?? '', port: raw?.port ?? raw?.Port ?? 0, id: raw?.id ?? raw?.ID ?? raw?.Id ?? raw?.name ?? raw?.Name ?? '', codec: raw?.codec ?? raw?.Codec ?? '', sampleRate: raw?.sampleRate ?? raw?.SampleRate ?? 48000, channels: raw?.channels ?? raw?.Channels ?? 2, bitrate: raw?.bitrate ?? raw?.Bitrate ?? 128000, frameMs: raw?.frameMs ?? raw?.FrameMs ?? 10, supportedFrameMs: supportedFrameMs.length ? supportedFrameMs : [10], updatedAtMs: Number(raw?.updatedAtMs ?? raw?.UpdatedAtMs) || 0, settingsDeviceId: raw?.settingsDeviceId ?? raw?.SettingsDeviceID ?? '' };
}
function normalizeDeviceStatus(raw: any): DeviceStatus {
  const route = raw?.channelRoute ?? raw?.ChannelRoute ?? 'stereo';
  const channelRoute: ChannelRoute = (route === 'left' || route === 'right') ? route : 'stereo';
  return { deviceId: String(raw?.deviceId ?? raw?.DeviceID ?? ''), name: String(raw?.name ?? raw?.Name ?? ''), connected: Boolean(raw?.connected ?? raw?.Connected), message: String(raw?.message ?? raw?.Message ?? ''), bitrate: Number(raw?.bitrate ?? raw?.Bitrate) || 0, frameMs: Number(raw?.frameMs ?? raw?.FrameMs) || 0, phase: Number(raw?.phase ?? raw?.Phase) || 0, channelRoute };
}
// 旧版接收端不上报校准阶段：停留“检测”超过 6 秒视为已在正常播放。
const nowTick = ref(Date.now());
let tickTimer: number | undefined;
function devicePhase(device: Device): number {
  const entry = calibration.value[device.id];
  if (!entry) return 3;
  if (entry.phase === 0 && nowTick.value - entry.updatedAt > 6000) return 3;
  return entry.phase;
}
function deviceCalibrating(device: Device): boolean {
  return !!connected.value[device.id] && devicePhase(device) < 3;
}
const calibratingCount = computed(() => connectedDevices.value.filter(deviceCalibrating).length);
const allCalibrated = computed(() => connectedCount.value > 0 && connectedDevices.value.every((device) => devicePhase(device) >= 3));
watch(allCalibrated, (now, was) => {
  if (!now || was) return;
  syncFlash.value = true;
});
function showSyncPanel(): boolean { return calibratingCount.value > 0 || (syncFlash.value && connectedCount.value > 0); }
function syncSummary(): string {
  return connectedDevices.value.map((device) => t('sync.summaryItem', { name: device.name, offset: Math.abs(calibration.value[device.id]?.offsetMs ?? 0) })).join(' · ');
}
function newer(a: { updatedAtMs: number; deviceId: string }, b: { updatedAtMs: number; deviceId: string }) { return a.updatedAtMs > b.updatedAtMs || (a.updatedAtMs === b.updatedAtMs && a.deviceId > b.deviceId); }
function touch(next: Partial<Settings>) { settings.value = { ...settings.value, ...next, updatedAtMs: Date.now(), deviceId }; }
function statusMessage(raw: any): string {
  const value = raw?.message ?? raw?.Message;
  return typeof value === 'string' && value.trim() ? value : '';
}
function deviceInfo(device: Device): string {
  const value = connected.value[device.id];
  if (!value) return '';
  if (value.message && value.message !== STREAMING_CODE) return translateBackend(value.message);
  return value.bitrate ? `${value.bitrate / 1000} kbps` : '';
}
function deviceStalled(device: Device): boolean {
  const value = connected.value[device.id];
  return !!value && !!value.message && value.message !== STREAMING_CODE;
}
function applyTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme;
  if (theme === 'dark') WindowSetDarkTheme(); else if (theme === 'light') WindowSetLightTheme(); else WindowSetSystemDefaultTheme();
}
async function discover() { devices.value = []; try { await DiscoverDevices(); } catch { setStatus('status.discoverFailed'); } }
async function refreshAuthorized() {
  try {
    const list: any = await ListAuthorizedDevices();
    authorizedDevices.value = (Array.isArray(list) ? list : []).map((raw: any) => ({ ID: String(raw?.ID ?? raw?.id ?? ''), Name: String(raw?.Name ?? raw?.name ?? ''), AddedAtMs: Number(raw?.AddedAtMs ?? raw?.addedAtMs) || 0 }));
  } catch { authorizedDevices.value = []; }
}
async function respondConnection(allow: boolean) {
  const request = connRequests.value[0];
  if (!request) return;
  connRequests.value = connRequests.value.slice(1);
  try {
    await RespondConnection(request.requestId, allow, rememberChoice.value);
    if (allow) setStatus('status.allowed', { name: request.name });
  } catch (error) { setErrorStatus('status.respondFailed', error); }
}
async function removeAuthorized(device: AuthorizedDevice) {
  try { await RemoveAuthorizedDevice(device.ID); await refreshAuthorized(); } catch { setStatus('status.removeFailed'); }
}
async function saveNtp() {
  try { await SaveNTPServer(ntpServer.value.trim() || 'ntp.aliyun.com'); ntpServer.value = ntpServer.value.trim() || 'ntp.aliyun.com'; ntpResult.value = t('settings.ntpSaved'); }
  catch { ntpResult.value = t('settings.ntpInvalid'); }
}
async function testNtp() {
  await saveNtp();
  try { const value: any = await TestNTPServer(); const reachable = Boolean(value?.reachable ?? value?.Reachable); ntpResult.value = reachable ? t('settings.ntpOffset', { offset: Math.abs(Number(value?.offsetMs ?? value?.OffsetMs) || 0) }) : t('settings.ntpUnavailable'); }
  catch { ntpResult.value = t('settings.ntpUnavailable'); }
}
async function connect(device: Device) {
  if (connected.value[device.id] || connecting.value[device.id]) return;
  if (!device.supportedFrameMs.includes(settings.value.frameMs)) { setStatus('status.frameUnsupported', { frame: settings.value.frameMs }); return; }
  connecting.value[device.id] = true;
  setStatus('status.waitingConfirm', { name: device.name });
  try {
    await Connect({ Name: device.name, Host: device.host, Port: device.port, ID: device.id, Codec: device.codec, SampleRate: device.sampleRate, Channels: device.channels, Bitrate: settings.value.bitrate, FrameMs: settings.value.frameMs, SupportedFrameMs: device.supportedFrameMs, UpdatedAtMs: settings.value.updatedAtMs, SettingsDeviceID: device.settingsDeviceId } as any);
    setStatus('');
  } catch (error) { setErrorStatus('status.connectFailed', error); }
  connecting.value[device.id] = false;
}
async function disconnect(device: Device) {
  try { await Disconnect(device.id); delete connected.value[device.id]; } catch { setStatus('status.disconnectFailed'); }
}
async function setChannelRoute(deviceId: string, route: ChannelRoute) {
  // Optimistically update local state for immediate UI feedback.
  const cur = connected.value[deviceId];
  if (cur) connected.value[deviceId] = { ...cur, channelRoute: route };
  try { await SetChannelRoute(deviceId, route); } catch (error) {
    // Revert on failure.
    if (cur) connected.value[deviceId] = cur;
    setErrorStatus('status.connectFailed', error);
  }
}
watch(settings, (next) => { localStorage.setItem(settingsKey, JSON.stringify(next)); applyTheme(next.theme); SaveLocalSettings(next.bitrate, next.frameMs).catch(() => {}); }, { deep: true });
watch(page, (next) => { if (next === 'settings') { refreshAuthorized(); GetNTPSettings().then((value: any) => { ntpServer.value = String(value?.server ?? value?.Server ?? 'ntp.aliyun.com'); }).catch(() => {}); GetIdentity().then((value: any) => { identity.value = { deviceId: String(value?.deviceId ?? value?.DeviceID ?? ''), name: String(value?.name ?? value?.Name ?? '') }; }).catch(() => {}); } });
watch(locale, () => { document.documentElement.lang = locale.value === 'zh' ? 'zh-CN' : 'en'; }, { immediate: true });
onMounted(async () => {
  try { settings.value = normalizeSettings(JSON.parse(localStorage.getItem(settingsKey) ?? 'null')); } catch { settings.value = { ...defaults }; }
  applyTheme(settings.value.theme);
  SaveLocalSettings(settings.value.bitrate, settings.value.frameMs).catch(() => {});
  try {
    const value: any = await GetStatus();
    const list: any[] = Array.isArray(value?.devices) ? value.devices : Array.isArray(value?.Devices) ? value.Devices : [];
    for (const item of list) { const entry = normalizeDeviceStatus(item); if (entry.connected && entry.deviceId) { connected.value[entry.deviceId] = entry; calibration.value[entry.deviceId] = { phase: entry.phase, offsetMs: 0, rttMs: 0, updatedAt: Date.now() }; } }
    if (!connectedCount.value) { const message = statusMessage(value); if (message) setStatusBackend(message); }
  } catch { setStatus('status.getStatusFailed'); }
  tickTimer = window.setInterval(() => { nowTick.value = Date.now(); }, 1000);
  EventsOn('device:found', (raw: any) => { const device = normalizeDevice(raw); if (device.updatedAtMs && newer(device, settings.value)) { settings.value = { ...settings.value, bitrate: device.bitrate, frameMs: device.frameMs, updatedAtMs: device.updatedAtMs, deviceId: device.settingsDeviceId || settings.value.deviceId }; } const index = devices.value.findIndex((item) => item.id === device.id); if (index >= 0) devices.value[index] = device; else devices.value.push(device); });
  EventsOn('device:lost', (raw: any) => {
    const id = String(raw ?? '');
    if (!id || connected.value[id]) return;
    devices.value = devices.value.filter((item) => item.id !== id);
  });
  EventsOn('stream:status', (raw: any) => {
    const value = normalizeDeviceStatus(raw);
    if (!value.deviceId) return;
    if (value.connected) {
      connected.value[value.deviceId] = value;
      calibration.value[value.deviceId] = { phase: value.phase, offsetMs: calibration.value[value.deviceId]?.offsetMs ?? 0, rttMs: calibration.value[value.deviceId]?.rttMs ?? 0, updatedAt: Date.now() };
    } else {
      delete connected.value[value.deviceId];
      delete calibration.value[value.deviceId];
      if (value.message) setStatusBackend(value.message);
    }
  });
  EventsOn('calibration:progress', (raw: any) => {
    const id = String(raw?.deviceId ?? raw?.DeviceID ?? '');
    if (!id || !connected.value[id]) return;
    calibration.value[id] = { phase: Math.min(3, Math.max(0, Number(raw?.phase ?? raw?.Phase ?? 0) || 0)), offsetMs: Number(raw?.offsetMs ?? raw?.OffsetMs ?? 0) || 0, rttMs: Number(raw?.rttMs ?? raw?.RttMs ?? 0) || 0, updatedAt: Date.now() };
  });
  EventsOn('conn:request', (raw: any) => { const request: ConnRequest = { requestId: String(raw?.requestId ?? raw?.RequestID ?? ''), deviceId: String(raw?.deviceId ?? raw?.DeviceID ?? ''), name: String(raw?.name ?? raw?.Name ?? '') || t('modal.unknownDevice'), host: String(raw?.host ?? raw?.Host ?? '') }; if (request.requestId && !connRequests.value.some((item) => item.requestId === request.requestId)) connRequests.value = [...connRequests.value, request]; rememberChoice.value = true; });
  EventsOn('conn:cancelled', (requestId: unknown) => { connRequests.value = connRequests.value.filter((item) => item.requestId !== String(requestId)); });
  discover();
});
onUnmounted(() => { window.clearInterval(tickTimer); });
</script>

<template>
  <main>
    <header>
      <div><p class="eyebrow">{{ t('header.eyebrow') }}</p><h1>Nexus</h1></div>
      <div class="header-actions"><span :class="['status', connectedCount ? 'live' : '']">{{ headerStatus }}</span><button class="secondary" @click="page = page === 'settings' ? 'devices' : 'settings'">{{ page === 'settings' ? t('header.back') : t('header.settings') }}</button></div>
    </header>

    <section v-if="page === 'devices'">
      <div class="section-head"><div><h2>{{ t('devices.title') }}</h2><p>{{ t('devices.hint') }}</p></div><button @click="discover">{{ t('devices.rescan') }}</button></div>
      <Transition name="fold">
        <div v-if="showSyncPanel()" class="sync-panel" :class="{ done: allCalibrated }">
          <div class="sync-head">
            <span class="pc-chip">PC</span>
            <div class="sync-title">
              <h3>{{ allCalibrated ? t('sync.doneTitle') : t('sync.busyTitle') }}</h3>
              <p>{{ allCalibrated ? t('sync.doneHint') : t('sync.busyHint') }}</p>
            </div>
            <span class="wave lg" aria-hidden="true"><i v-for="n in 14" :key="n" :style="{ animationDelay: `${n * 70}ms` }"></i></span>
          </div>
          <div class="node-rows">
            <div v-for="(device, index) in connectedDevices" :key="device.id" class="node-row">
              <span class="link-line" aria-hidden="true"><i class="pulse" :style="{ animationDelay: `${index * 240}ms` }"></i></span>
              <span class="node-chip" :class="'phase-' + devicePhase(device)">{{ (device.name.trim()[0] || 'S').toUpperCase() }}</span>
              <span class="node-meta">
                <strong>{{ device.name }}</strong>
                <span :class="{ ok: devicePhase(device) >= 3 }">{{ phaseHints[devicePhase(device)] }}<template v-if="devicePhase(device) >= 2 && calibration[device.id]"> · {{ t('calib.stats', { offset: Math.abs(calibration[device.id].offsetMs), rtt: calibration[device.id].rttMs }) }}</template></span>
              </span>
              <span class="mini-steps" aria-hidden="true"><i v-for="(label, i) in phaseLabels" :key="label" :class="{ active: i === devicePhase(device), done: i < devicePhase(device) }"></i></span>
            </div>
          </div>
        </div>
      </Transition>
      <div v-if="devices.length" class="devices"><article v-for="device in devices" :key="device.id" :class="{ live: connected[device.id] }"><div class="speaker">S</div><div><h3>{{ device.name }}</h3><p>{{ device.host }}:{{ device.port }} · {{ t('device.supports', { frames: device.supportedFrameMs.join('/') }) }}<span v-if="deviceInfo(device)" :class="['live-info', { warn: deviceStalled(device) }]"> · {{ deviceInfo(device) }}</span></p>
        <div v-if="connected[device.id]" class="channel-row">
          <span class="channel-label">{{ t('channel.label') }}</span>
          <div class="channel-group">
            <button
              type="button"
              :class="['channel-btn', { active: (connected[device.id].channelRoute || 'stereo') === 'left' }]"
              @click="setChannelRoute(device.id, 'left')"
            >{{ t('channel.left') }}</button>
            <button
              type="button"
              :class="['channel-btn', { active: (connected[device.id].channelRoute || 'stereo') === 'stereo' }]"
              @click="setChannelRoute(device.id, 'stereo')"
            >{{ t('channel.stereo') }}</button>
            <button
              type="button"
              :class="['channel-btn', { active: (connected[device.id].channelRoute || 'stereo') === 'right' }]"
              @click="setChannelRoute(device.id, 'right')"
            >{{ t('channel.right') }}</button>
          </div>
        </div>
        <Transition name="fade">
          <div v-if="connected[device.id] && devicePhase(device) < 3" class="calib-row">
            <span class="wave" aria-hidden="true"><i v-for="n in 10" :key="n" :style="{ animationDelay: `${n * 80}ms` }"></i></span>
            <ol class="calib-steps">
              <li v-for="(label, i) in phaseLabels" :key="label" :class="{ active: i === devicePhase(device), done: i < devicePhase(device) }">{{ label }}</li>
            </ol>
            <span class="calib-hint">{{ phaseHints[devicePhase(device)] }}</span>
          </div>
        </Transition>
      </div><button v-if="connected[device.id]" class="secondary" @click="disconnect(device)">{{ t('device.disconnect') }}</button><button v-else-if="connecting[device.id]" class="secondary" disabled>{{ t('device.waiting') }}</button><button v-else @click="connect(device)">{{ t('device.connect') }}</button></article></div>
      <div v-else class="empty"><span class="spinner"></span>{{ t('empty.scanning') }}<p class="empty-hint">{{ t('empty.hint') }}</p></div>
      <p v-if="connectedCount > 1 && !showSyncPanel() && allCalibrated" class="sync-note">{{ t('sync.note') }}</p>
    </section>

    <section v-else class="settings">
      <div class="section-head"><div><h2>{{ t('header.settings') }}</h2><p>{{ t('settings.hint') }}</p></div></div>
      <fieldset><legend>{{ t('settings.bitrate') }}</legend><label v-for="bitrate in [64000, 96000, 128000, 192000]" :key="bitrate" class="choice"><input v-model="settings.bitrate" @change="touch({ bitrate })" type="radio" name="bitrate" :value="bitrate"><span>{{ bitrate / 1000 }} kbps</span></label></fieldset>
      <fieldset><legend>{{ t('settings.frame') }}</legend><label v-for="frame in [10, 20]" :key="frame" class="choice" :class="{ disabled: !supportedFrames.includes(frame) }"><input v-model="settings.frameMs" @change="touch({ frameMs: frame })" type="radio" name="frame" :value="frame" :disabled="!supportedFrames.includes(frame)"><span>{{ frame }} ms</span></label><p v-if="!frameAvailable" class="warning">{{ t('settings.frameWarning') }}</p></fieldset>
      <fieldset><legend>{{ t('settings.ntp') }}</legend><p class="field-hint">{{ t('settings.ntpHint') }}</p><input v-model="ntpServer" class="text-input" type="text" inputmode="url" autocomplete="off" @change="saveNtp"><div class="ntp-actions"><button class="secondary" @click="ntpServer = 'ntp.aliyun.com'; saveNtp()">{{ t('settings.ntpDefault') }}</button><button @click="testNtp">{{ t('settings.ntpTest') }}</button></div><p v-if="ntpResult" class="field-hint">{{ ntpResult }}</p></fieldset>
      <fieldset><legend>{{ t('settings.appearance') }}</legend><label v-for="theme in themeOptions" :key="theme" class="choice"><input v-model="settings.theme" type="radio" name="theme" :value="theme"><span>{{ t('theme.' + theme) }}</span></label></fieldset>
      <fieldset><legend>{{ t('settings.language') }}</legend><label v-for="lang in languageOptions" :key="lang" class="choice"><input type="radio" name="language" :value="lang" :checked="language === lang" @change="setLanguage(lang)"><span>{{ t('language.' + lang) }}</span></label></fieldset>
      <fieldset><legend>{{ t('settings.authorized') }}</legend>
        <p class="field-hint">{{ t('settings.authorizedHint') }}</p>
        <div v-if="authorizedDevices.length" class="authorized">
          <div v-for="device in authorizedDevices" :key="device.ID" class="authorized-row">
            <div><strong>{{ device.Name || t('device.unnamed') }}</strong><span class="muted">{{ device.ID }}</span></div>
            <button class="secondary danger" @click="removeAuthorized(device)">{{ t('device.remove') }}</button>
          </div>
        </div>
        <p v-else class="field-hint">{{ t('settings.authorizedEmpty') }}</p>
      </fieldset>
      <fieldset class="readonly"><legend>{{ t('settings.localInfo') }}</legend>
        <p>{{ t('info.name', { value: identity.name || t('info.unnamed') }) }}</p>
        <p>{{ t('info.id', { value: identity.deviceId }) }}</p>
        <p>{{ t('info.codec') }}</p><p>{{ t('info.sampleRate') }}</p><p>{{ t('info.channels') }}</p>
      </fieldset>
    </section>
    <footer>{{ connectedCount ? t('footer.connectedPrefix', { n: connectedCount }) : '' }}{{ settings.bitrate / 1000 }} kbps · {{ settings.frameMs }} ms · {{ t('footer.tail') }}</footer>

    <div v-if="connRequests.length" class="modal-overlay">
      <div class="modal" role="dialog" aria-modal="true">
        <h3>{{ t('modal.title') }}<span v-if="connRequests.length > 1" class="modal-count">{{ t('modal.morePending', { n: connRequests.length - 1 }) }}</span></h3>
        <p class="modal-device">{{ connRequests[0].name }}</p>
        <p class="muted">{{ connRequests[0].host }} {{ t('modal.desc') }}</p>
        <label class="choice"><input v-model="rememberChoice" type="checkbox"><span>{{ t('modal.remember') }}</span></label>
        <div class="modal-actions">
          <button class="secondary" @click="respondConnection(false)">{{ t('modal.deny') }}</button>
          <button @click="respondConnection(true)">{{ t('modal.allow') }}</button>
        </div>
      </div>
    </div>
  </main>
</template>

<style>
:root { font-family: Inter, system-ui, sans-serif; color: #172033; background: #f6f7f8; }
:root[data-theme='dark'] { color: #e8edf0; background: #172033; color-scheme: dark; }
:root[data-theme='light'] { color-scheme: light; }
body { margin: 0; background: inherit; } main { max-width: 940px; margin: auto; padding: 44px 32px; } header, .section-head, article, .header-actions { display: flex; align-items: center; justify-content: space-between; gap: 18px; } header { border-bottom: 1px solid #d9dde1; padding-bottom: 29px; }.eyebrow { color: #687077; font-size: 12px; font-weight: 700; margin: 0 0 5px; } h1 { font-size: 32px; margin: 0; } h2 { margin: 0; font-size: 21px; } h3 { margin: 0 0 5px; } p { color: #687077; margin: 0; }.status { font-size: 14px; padding: 8px 12px; border: 1px solid #d9dde1; background: #fff; }.status.live { border-color: #16805b; color: #086d4d; } .live-info { color: #086d4d; font-weight: 600; } .live-info.warn { color: #a24a18; } section { margin-top: 42px; }.section-head { margin-bottom: 16px; } button { border: 0; background: #086d4d; color: #fff; padding: 10px 15px; font-weight: 650; cursor: pointer; }.secondary { background: #e8ecee; color: #172033; }.devices { border-top: 1px solid #d9dde1; } article { background: #fff; border-bottom: 1px solid #d9dde1; padding: 18px; }.speaker { width: 34px; height: 34px; display: grid; place-items: center; background: #f0c75e; color: #172033; font-weight: 800; } article > div:nth-child(2) { flex: 1; }.empty { border: 1px dashed #b8c0c6; padding: 32px; color: #687077; } fieldset { border: 1px solid #d9dde1; margin: 0 0 18px; padding: 14px; } legend { font-weight: 700; padding: 0 5px; }.choice { display: flex; align-items: center; gap: 9px; padding: 9px 4px; cursor: pointer; }.disabled { color: #8b949a; cursor: not-allowed; }.warning { color: #a24a18; font-size: 13px; }.readonly p { padding: 4px 0; } footer { margin-top: 44px; color: #687077; font-size: 13px; }
:root[data-theme='dark'] header, :root[data-theme='dark'] .devices, :root[data-theme='dark'] article, :root[data-theme='dark'] fieldset { border-color: #3d4a51; }:root[data-theme='dark'] .status, :root[data-theme='dark'] article { background: #202b31; }:root[data-theme='dark'] .secondary { background: #36434a; color: #e8edf0; }:root[data-theme='dark'] p, :root[data-theme='dark'] footer { color: #b6c1c7; }:root[data-theme='dark'] .live-info { color: #4fc08d; }
@media (prefers-color-scheme: dark) {
  :root[data-theme='system'] { color: #e8edf0; background: #172033; color-scheme: dark; }
  :root[data-theme='system'] header, :root[data-theme='system'] .devices, :root[data-theme='system'] article, :root[data-theme='system'] fieldset { border-color: #3d4a51; }
  :root[data-theme='system'] .status, :root[data-theme='system'] article { background: #202b31; }
  :root[data-theme='system'] .secondary { background: #36434a; color: #e8edf0; }
  :root[data-theme='system'] p, :root[data-theme='system'] footer { color: #b6c1c7; }
  :root[data-theme='system'] .live-info { color: #4fc08d; }
}
@media (max-width: 600px) { main { padding: 26px 18px; } header { align-items: flex-start; flex-wrap: wrap; }.header-actions, .section-head, article { align-items: flex-start; flex-wrap: wrap; } }
article.live { border-left: 3px solid #16805b; } article.live .speaker { background: #086d4d; color: #fff; }
button[disabled] { opacity: 0.6; cursor: default; }
.empty { display: flex; flex-direction: column; align-items: center; gap: 10px; border: 1px dashed #b8c0c6; padding: 36px 32px; color: #687077; }
.empty-hint { font-size: 13px; } .empty-hint::before { content: ''; }
.spinner { width: 18px; height: 18px; border: 2px solid #d9dde1; border-top-color: #086d4d; border-radius: 50%; animation: spin 0.9s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
.sync-note { margin-top: 14px; font-size: 13px; color: #086d4d; background: #eaf5f0; border: 1px solid #c8e5d8; padding: 8px 12px; }
/* ---- 多设备同步校准动画 ---- */
.sync-panel { border: 1px solid #c8e5d8; background: linear-gradient(180deg, #f2faf6, #eaf5f0); padding:  16px 18px; margin-bottom: 16px; overflow: hidden; }
.sync-head { display: flex; align-items: center; gap: 14px; }
.sync-title { flex: 1; min-width: 0; }
.sync-title h3 { font-size: 16px; margin: 0 0 3px; color: #086d4d; }
.sync-title p { font-size: 13px; }
.pc-chip { width: 38px; height: 38px; flex: none; display: grid; place-items: center; border-radius: 10px; background: #172033; color: #f0c75e; font-weight: 800; font-size: 13px; letter-spacing: 0.5px; }
.node-rows { margin-top: 14px; display: flex; flex-direction: column; gap: 8px; }
.node-row { display: flex; align-items: center; gap: 12px; }
.link-line { position: relative; width: 46px; height: 2px; flex: none; background: linear-gradient(90deg, #b7dcc9, #d6ece0); border-radius: 1px; overflow: visible; }
.link-line .pulse { position: absolute; top: 50%; left: 0; width: 7px; height: 7px; margin: -3.5px 0 0 -3.5px; border-radius: 50%; background: #086d4d; box-shadow: 0 0 6px rgba(8, 109, 77, 0.55); animation: travel 1.5s cubic-bezier(0.45, 0, 0.55, 1) infinite; }
@keyframes travel { 0% { left: 0; opacity: 0; } 12% { opacity: 1; } 88% { opacity: 1; } 100% { left: 100%; opacity: 0; } }
.node-chip { width: 30px; height: 30px; flex: none; display: grid; place-items: center; border-radius: 50%; font-size: 13px; font-weight: 700; color: #4d6a60; background: #e3ece8; border: 1.5px solid #c3d7cf; transition: background 0.5s, color 0.5s, border-color 0.5s, box-shadow 0.5s; }
.node-chip.phase-1 { color: #086d4d; border-color: #4fc08d; }
.node-chip.phase-2 { color: #086d4d; border-color: #086d4d; box-shadow: 0 0 0 4px rgba(8, 109, 77, 0.12); }
.node-chip.phase-3 { background: #086d4d; color: #fff; border-color: #086d4d; box-shadow: 0 0 0 5px rgba(8, 109, 77, 0.16); }
.node-meta { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.node-meta strong { font-size: 14px; }
.node-meta span { font-size: 12px; color: #687077; }
.node-meta span.ok { color: #086d4d; }
.mini-steps { display: flex; gap: 5px; flex: none; }
.mini-steps i { width: 8px; height: 8px; border-radius: 50%; background: #cfe0d8; border: 1px solid #b7d2c6; transition: background 0.4s, transform 0.4s; }
.mini-steps i.done { background: #4fc08d; border-color: #4fc08d; }
.mini-steps i.active { background: #086d4d; border-color: #086d4d; transform: scale(1.25); animation: breathe 1.1s ease-in-out infinite; }
@keyframes breathe { 50% { box-shadow: 0 0 0 4px rgba(8, 109, 77, 0.15); } }
.sync-panel.done .mini-steps i { animation: sync-dot-pulse 1.2s ease-in-out infinite; }
.sync-panel.done .mini-steps i:nth-child(2) { animation-delay: 150ms; }
.sync-panel.done .mini-steps i:nth-child(3) { animation-delay: 300ms; }
.sync-panel.done .mini-steps i:nth-child(4) { animation-delay: 450ms; }
@keyframes sync-dot-pulse {
  0%, 100% { opacity: 0.65; transform: scale(0.9); box-shadow: 0 0 0 0 rgba(8, 109, 77, 0); }
  45% { opacity: 1; transform: scale(1.3); box-shadow: 0 0 0 3px rgba(8, 109, 77, 0.16); }
}
.wave { display: inline-flex; align-items: center; gap: 3px; height: 20px; flex: none; }
.wave i { width: 3px; height: 6px; border-radius: 2px; background: #086d4d; opacity: 0.75; animation: wavebar 1s ease-in-out infinite; }
.wave.lg { height: 30px; gap: 4px; }
.wave.lg i { width: 4px; background: #086d4d; }
@keyframes wavebar { 0%, 100% { transform: scaleY(0.45); } 50% { transform: scaleY(1.9); } }
.sync-panel.done .wave i, .sync-panel.done .link-line .pulse { animation-play-state: running; opacity: 0.75; }
.sync-panel.done .node-chip.phase-3 { animation: breathe 1.1s ease-in-out infinite; }
.calib-row { display: flex; align-items: center; gap: 10px; margin-top: 8px; flex-wrap: wrap; }
.calib-steps { display: flex; gap: 4px; list-style: none; margin: 0; padding: 0; }
.calib-steps li { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: #e8ecee; color: #687077; transition: background 0.4s, color 0.4s; }
.calib-steps li.done { background: #dcefe6; color: #086d4d; }
.calib-steps li.active { background: #086d4d; color: #fff; animation: phasepulse 1.2s ease-in-out infinite; }
@keyframes phasepulse { 50% { box-shadow: 0 0 0 3px rgba(8, 109, 77, 0.18); } }
.calib-hint { font-size: 11px; color: #687077; }
.channel-row { display: flex; align-items: center; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.channel-label { font-size: 12px; color: #687077; font-weight: 600; }
.channel-group { display: inline-flex; border: 1px solid #d9dde1; border-radius: 4px; overflow: hidden; background: #fff; }
.channel-btn { border: 0; background: transparent; color: #172033; padding: 4px 10px; font-size: 12px; font-weight: 500; cursor: pointer; border-radius: 0; transition: background 0.2s, color 0.2s; }
.channel-btn:not(:last-child) { border-right: 1px solid #d9dde1; }
.channel-btn.active { background: #086d4d; color: #fff; font-weight: 700; }
.channel-btn:hover:not(.active) { background: #e8ecee; }
.fold-enter-active, .fold-leave-active { transition: opacity 0.5s ease, transform 0.5s ease; }
.fold-enter-from, .fold-leave-to { opacity: 0; transform: translateY(-6px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.6s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.field-hint { font-size: 13px; margin-bottom: 8px; } .muted { color: #687077; font-size: 12px; }
.text-input { box-sizing: border-box; width: min(420px, 100%); padding: 9px 10px; border: 1px solid #b8c0c6; background: #fff; color: #172033; font: inherit; } .ntp-actions { display: flex; gap: 8px; margin-top: 10px; }
.authorized { display: flex; flex-direction: column; gap: 6px; } .authorized-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 9px 10px; border: 1px solid #d9dde1; background: #fff; } .authorized-row .muted { display: block; } .authorized-row div { min-width: 0; } .danger { color: #a24a18; }
.modal-overlay { position: fixed; inset: 0; background: rgba(23, 32, 51, 0.45); display: grid; place-items: center; z-index: 40; }
.modal { background: #fff; color: #172033; border-radius: 10px; padding: 24px; width: min(420px, calc(100vw - 48px)); box-shadow: 0 18px 50px rgba(0, 0, 0, 0.25); }
.modal h3 { margin: 0 0 8px; } .modal-count { font-size: 13px; font-weight: 400; color: #687077; } .modal-device { font-size: 17px; font-weight: 700; margin: 0 0 4px; } .modal .muted { font-size: 13px; margin-bottom: 14px; } .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
:root[data-theme='dark'] .authorized-row { border-color: #3d4a51; background: #202b31; } :root[data-theme='dark'] .modal { background: #202b31; color: #e8edf0; } :root[data-theme='dark'] .modal .muted { color: #b6c1c7; }
:root[data-theme='dark'] .text-input { background: #202b31; color: #e8edf0; border-color: #3d4a51; }
:root[data-theme='dark'] .sync-note { color: #4fc08d; background: #1c2f28; border-color: #2b4a3c; } :root[data-theme='dark'] .spinner { border-color: #3d4a51; border-top-color: #4fc08d; }
:root[data-theme='dark'] .sync-panel { border-color: #2b4a3c; background: linear-gradient(180deg, #1d2b26, #1a2622); }
:root[data-theme='dark'] .sync-title h3 { color: #4fc08d; }
:root[data-theme='dark'] .link-line { background: linear-gradient(90deg, #2f5546, #244034); }
:root[data-theme='dark'] .link-line .pulse { background: #4fc08d; box-shadow: 0 0 6px rgba(79, 192, 141, 0.55); }
:root[data-theme='dark'] .node-chip { color: #9db8ad; background: #22302b; border-color: #33503f; }
:root[data-theme='dark'] .node-chip.phase-1 { color: #4fc08d; border-color: #4fc08d; }
:root[data-theme='dark'] .node-chip.phase-2 { color: #4fc08d; border-color: #4fc08d; box-shadow: 0 0 0 4px rgba(79, 192, 141, 0.14); }
:root[data-theme='dark'] .node-chip.phase-3 { background: #086d4d; color: #eafff5; border-color: #4fc08d; box-shadow: 0 0 0 5px rgba(79, 192, 141, 0.18); }
:root[data-theme='dark'] .node-meta span.ok { color: #4fc08d; }
:root[data-theme='dark'] .mini-steps i { background: #24352d; border-color: #33503f; }
:root[data-theme='dark'] .mini-steps i.done { background: #2f6b4f; border-color: #2f6b4f; }
:root[data-theme='dark'] .mini-steps i.active { background: #4fc08d; border-color: #4fc08d; }
:root[data-theme='dark'] .wave i { background: #4fc08d; }
:root[data-theme='dark'] .calib-steps li { background: #242f34; color: #b6c1c7; }
:root[data-theme='dark'] .calib-steps li.done { background: #1f3a2e; color: #4fc08d; }
:root[data-theme='dark'] .calib-steps li.active { background: #086d4d; color: #eafff5; }
:root[data-theme='dark'] .channel-label { color: #b6c1c7; }
:root[data-theme='dark'] .channel-group { border-color: #3d4a51; background: #202b31; }
:root[data-theme='dark'] .channel-btn { color: #e8edf0; }
:root[data-theme='dark'] .channel-btn:not(:last-child) { border-right-color: #3d4a51; }
:root[data-theme='dark'] .channel-btn.active { background: #086d4d; color: #fff; }
:root[data-theme='dark'] .channel-btn:hover:not(.active) { background: #36434a; }
@media (prefers-color-scheme: dark) {
  :root[data-theme='system'] .authorized-row { border-color: #3d4a51; background: #202b31; }
  :root[data-theme='system'] .text-input { background: #202b31; color: #e8edf0; border-color: #3d4a51; }
  :root[data-theme='system'] .modal { background: #202b31; color: #e8edf0; }
  :root[data-theme='system'] .modal .muted { color: #b6c1c7; }
  :root[data-theme='system'] .sync-note { color: #4fc08d; background: #1c2f28; border-color: #2b4a3c; }
  :root[data-theme='system'] .spinner { border-color: #3d4a51; border-top-color: #4fc08d; }
  :root[data-theme='system'] .sync-panel { border-color: #2b4a3c; background: linear-gradient(180deg, #1d2b26, #1a2622); }
  :root[data-theme='system'] .sync-title h3 { color: #4fc08d; }
  :root[data-theme='system'] .link-line { background: linear-gradient(90deg, #2f5546, #244034); }
  :root[data-theme='system'] .link-line .pulse { background: #4fc08d; box-shadow: 0 0 6px rgba(79, 192, 141, 0.55); }
  :root[data-theme='system'] .node-chip { color: #9db8ad; background: #22302b; border-color: #33503f; }
  :root[data-theme='system'] .node-chip.phase-1 { color: #4fc08d; border-color: #4fc08d; }
  :root[data-theme='system'] .node-chip.phase-2 { color: #4fc08d; border-color: #4fc08d; box-shadow: 0 0 0 4px rgba(79, 192, 141, 0.14); }
  :root[data-theme='system'] .node-chip.phase-3 { background: #086d4d; color: #eafff5; border-color: #4fc08d; box-shadow: 0 0 0 5px rgba(79, 192, 141, 0.18); }
  :root[data-theme='system'] .node-meta span.ok { color: #4fc08d; }
  :root[data-theme='system'] .mini-steps i { background: #24352d; border-color: #33503f; }
  :root[data-theme='system'] .mini-steps i.done { background: #2f6b4f; border-color: #2f6b4f; }
  :root[data-theme='system'] .mini-steps i.active { background: #4fc08d; border-color: #4fc08d; }
  :root[data-theme='system'] .wave i { background: #4fc08d; }
  :root[data-theme='system'] .calib-steps li { background: #242f34; color: #b6c1c7; }
  :root[data-theme='system'] .calib-steps li.done { background: #1f3a2e; color: #4fc08d; }
  :root[data-theme='system'] .calib-steps li.active { background: #086d4d; color: #eafff5; }
  :root[data-theme='system'] .channel-label { color: #b6c1c7; }
  :root[data-theme='system'] .channel-group { border-color: #3d4a51; background: #202b31; }
  :root[data-theme='system'] .channel-btn { color: #e8edf0; }
  :root[data-theme='system'] .channel-btn:not(:last-child) { border-right-color: #3d4a51; }
  :root[data-theme='system'] .channel-btn.active { background: #086d4d; color: #fff; }
  :root[data-theme='system'] .channel-btn:hover:not(.active) { background: #36434a; }
}
</style>

