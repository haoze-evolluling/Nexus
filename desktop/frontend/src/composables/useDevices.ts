import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import type {
  Calibration,
  ChannelRoute,
  Device,
  DeviceStatus,
  ParamValue,
  Settings,
  StatusInfo,
} from '../types';
import {
  Connect,
  DiscoverDevices,
  Disconnect,
  GetStatus,
  SetChannelRoute,
} from '../../wailsjs/go/main/App';
import { EventsOn } from '../../wailsjs/runtime/runtime';
import { STREAMING_CODE, t, translateBackend } from '../i18n';

function normalizeDevice(raw: any): Device {
  const values =
    raw?.supportedFrameMs ?? raw?.SupportedFrameMs ?? [raw?.frameMs ?? raw?.FrameMs ?? 10];
  const supportedFrameMs = (Array.isArray(values) ? values : [values]).filter(
    (value) => value === 10 || value === 20
  );
  return {
    name: raw?.name ?? raw?.Name ?? 'Android device',
    host: raw?.host ?? raw?.Host ?? '',
    port: raw?.port ?? raw?.Port ?? 0,
    id: raw?.id ?? raw?.ID ?? raw?.Id ?? raw?.name ?? raw?.Name ?? '',
    codec: raw?.codec ?? raw?.Codec ?? '',
    sampleRate: raw?.sampleRate ?? raw?.SampleRate ?? 48000,
    channels: raw?.channels ?? raw?.Channels ?? 2,
    bitrate: raw?.bitrate ?? raw?.Bitrate ?? 128000,
    frameMs: raw?.frameMs ?? raw?.FrameMs ?? 10,
    supportedFrameMs: supportedFrameMs.length ? supportedFrameMs : [10],
    updatedAtMs: Number(raw?.updatedAtMs ?? raw?.UpdatedAtMs) || 0,
    settingsDeviceId: raw?.settingsDeviceId ?? raw?.SettingsDeviceID ?? '',
  };
}

function normalizeDeviceStatus(raw: any): DeviceStatus {
  const route = raw?.channelRoute ?? raw?.ChannelRoute ?? 'stereo';
  const channelRoute: ChannelRoute =
    route === 'left' || route === 'right' ? route : 'stereo';
  return {
    deviceId: String(raw?.deviceId ?? raw?.DeviceID ?? ''),
    name: String(raw?.name ?? raw?.Name ?? ''),
    connected: Boolean(raw?.connected ?? raw?.Connected),
    message: String(raw?.message ?? raw?.Message ?? ''),
    bitrate: Number(raw?.bitrate ?? raw?.Bitrate) || 0,
    frameMs: Number(raw?.frameMs ?? raw?.FrameMs) || 0,
    phase: Number(raw?.phase ?? raw?.Phase) || 0,
    channelRoute,
  };
}

function newer(
  a: { updatedAtMs: number; deviceId: string },
  b: { updatedAtMs: number; deviceId: string }
) {
  return (
    a.updatedAtMs > b.updatedAtMs ||
    (a.updatedAtMs === b.updatedAtMs && a.deviceId > b.deviceId)
  );
}

export function useDevices(
  settings: () => Settings,
  onNewerSettings?: (device: Device) => void
) {
  const devices = ref<Device[]>([]);
  const connected = ref<Record<string, DeviceStatus>>({});
  const connecting = ref<Record<string, boolean>>({});
  const calibration = ref<Record<string, Calibration>>({});
  const statusInfo = ref<StatusInfo>(null);
  const isScanning = ref(false);
  const syncFlash = ref(false);
  const nowTick = ref(Date.now());

  let tickTimer: number | undefined;

  const status = computed(() => {
    const info = statusInfo.value;
    if (!info) return t('status.idle');
    return info.kind === 'key' ? t(info.value, info.params) : translateBackend(info.value);
  });

  function setStatus(key: string, params?: Record<string, ParamValue>) {
    statusInfo.value = { kind: 'key', value: key, params };
  }

  function setStatusBackend(raw: string) {
    statusInfo.value = { kind: 'backend', value: raw };
  }

  function setErrorStatus(prefixKey: string, error: unknown) {
    const message = error instanceof Error ? error.message : String(error);
    if (message.startsWith('nxmsg:') || message.startsWith('svmsg:')) {
      setStatusBackend(message);
    } else {
      setStatus(prefixKey, { detail: message });
    }
  }

  const connectedCount = computed(() => Object.keys(connected.value).length);
  const connectedDevices = computed(() =>
    devices.value.filter((device) => connected.value[device.id])
  );

  const headerStatus = computed(() => {
    if (connectedCount.value > 0) {
      return t(connectedCount.value > 1 ? 'header.multi' : 'header.single', {
        n: connectedCount.value,
      });
    }
    return status.value;
  });

  const supportedFrames = computed(() => {
    if (connectedDevices.value.length) {
      return connectedDevices.value.reduce(
        (acc: number[], device) =>
          acc.filter((frame) => device.supportedFrameMs.includes(frame)),
        [10, 20]
      );
    }
    return devices.value.length
      ? Array.from(new Set(devices.value.flatMap((device) => device.supportedFrameMs)))
      : [10, 20];
  });

  const frameAvailable = computed(() =>
    connectedDevices.value.every((device) =>
      device.supportedFrameMs.includes(settings().frameMs)
    )
  );

  function devicePhase(device: Device): number {
    const entry = calibration.value[device.id];
    if (!entry) return 3;
    if (entry.phase === 0 && nowTick.value - entry.updatedAt > 6000) return 3;
    return entry.phase;
  }

  function deviceCalibrating(device: Device): boolean {
    return !!connected.value[device.id] && devicePhase(device) < 3;
  }

  const calibratingCount = computed(
    () => connectedDevices.value.filter(deviceCalibrating).length
  );

  const allCalibrated = computed(
    () =>
      connectedCount.value > 0 &&
      connectedDevices.value.every((device) => devicePhase(device) >= 3)
  );

  watch(allCalibrated, (now, was) => {
    if (!now || was) return;
    syncFlash.value = true;
  });

  const showSyncPanel = computed(() => {
    return calibratingCount.value > 0 || (syncFlash.value && connectedCount.value > 0);
  });

  function syncSummary(): string {
    return connectedDevices.value
      .map((device) =>
        t('sync.summaryItem', {
          name: device.name,
          offset: Math.abs(calibration.value[device.id]?.offsetMs ?? 0),
        })
      )
      .join(' · ');
  }

  function deviceInfo(device: Device): string {
    const value = connected.value[device.id];
    if (!value) return '';
    if (value.message && value.message !== STREAMING_CODE) {
      return translateBackend(value.message);
    }
    return value.bitrate ? `${value.bitrate / 1000} kbps` : '';
  }

  function deviceStalled(device: Device): boolean {
    const value = connected.value[device.id];
    return !!value && !!value.message && value.message !== STREAMING_CODE;
  }

  async function discover() {
    isScanning.value = true;
    devices.value = [];
    try {
      await DiscoverDevices();
    } catch {
      setStatus('status.discoverFailed');
    } finally {
      setTimeout(() => {
        isScanning.value = false;
      }, 1200);
    }
  }

  async function connect(device: Device) {
    if (connected.value[device.id] || connecting.value[device.id]) return;
    const curSettings = settings();
    if (!device.supportedFrameMs.includes(curSettings.frameMs)) {
      setStatus('status.frameUnsupported', { frame: curSettings.frameMs });
      return;
    }
    connecting.value[device.id] = true;
    setStatus('status.waitingConfirm', { name: device.name });
    try {
      await Connect({
        Name: device.name,
        Host: device.host,
        Port: device.port,
        ID: device.id,
        Codec: device.codec,
        SampleRate: device.sampleRate,
        Channels: device.channels,
        Bitrate: curSettings.bitrate,
        FrameMs: curSettings.frameMs,
        SupportedFrameMs: device.supportedFrameMs,
        UpdatedAtMs: curSettings.updatedAtMs,
        SettingsDeviceID: device.settingsDeviceId,
      } as any);
      setStatus('');
    } catch (error) {
      setErrorStatus('status.connectFailed', error);
    } finally {
      connecting.value[device.id] = false;
    }
  }

  async function disconnect(device: Device) {
    try {
      await Disconnect(device.id);
      delete connected.value[device.id];
      delete calibration.value[device.id];
    } catch {
      setStatus('status.disconnectFailed');
    }
  }

  async function setChannelRoute(deviceId: string, route: ChannelRoute) {
    const cur = connected.value[deviceId];
    if (cur) {
      connected.value[deviceId] = { ...cur, channelRoute: route };
    }
    try {
      await SetChannelRoute(deviceId, route);
    } catch (error) {
      if (cur) {
        connected.value[deviceId] = cur;
      }
      setErrorStatus('status.connectFailed', error);
    }
  }

  async function initDevices() {
    try {
      const value: any = await GetStatus();
      const list: any[] = Array.isArray(value?.devices)
        ? value.devices
        : Array.isArray(value?.Devices)
        ? value.Devices
        : [];
      for (const item of list) {
        const entry = normalizeDeviceStatus(item);
        if (entry.connected && entry.deviceId) {
          connected.value[entry.deviceId] = entry;
          calibration.value[entry.deviceId] = {
            phase: entry.phase,
            offsetMs: 0,
            rttMs: 0,
            updatedAt: Date.now(),
          };
        }
      }
      if (!connectedCount.value) {
        const message = value?.message ?? value?.Message;
        if (typeof message === 'string' && message.trim()) {
          setStatusBackend(message);
        }
      }
    } catch {
      setStatus('status.getStatusFailed');
    }

    tickTimer = window.setInterval(() => {
      nowTick.value = Date.now();
    }, 1000);

    EventsOn('device:found', (raw: any) => {
      const device = normalizeDevice(raw);
      if (device.updatedAtMs && newer(device, settings())) {
        onNewerSettings?.(device);
      }
      const index = devices.value.findIndex((item) => item.id === device.id);
      if (index >= 0) {
        devices.value[index] = device;
      } else {
        devices.value.push(device);
      }
    });

    EventsOn('device:lost', (raw: any) => {
      const id = String(raw ?? '');
      if (!id || connected.value[id]) return;
      devices.value = devices.value.filter((item) => item.id !== id);
    });

    EventsOn('stream:status', (raw: any) => {
      const value = normalizeDeviceStatus(raw);
      if (!value.deviceId) return;
      if (value.connected) {
        if (!devices.value.some((d) => d.id === value.deviceId)) {
          devices.value.push({
            name: value.name || 'Android device',
            host: '',
            port: 40125,
            id: value.deviceId,
            codec: 'opus',
            sampleRate: 48000,
            channels: 2,
            bitrate: value.bitrate || 128000,
            frameMs: value.frameMs || 10,
            supportedFrameMs: [10, 20],
            updatedAtMs: 0,
            settingsDeviceId: '',
          });
        }
        connected.value[value.deviceId] = value;
        calibration.value[value.deviceId] = {
          phase: value.phase,
          offsetMs: calibration.value[value.deviceId]?.offsetMs ?? 0,
          rttMs: calibration.value[value.deviceId]?.rttMs ?? 0,
          updatedAt: Date.now(),
        };
      } else {
        delete connected.value[value.deviceId];
        delete calibration.value[value.deviceId];
        if (value.message) {
          setStatusBackend(value.message);
        }
      }
    });

    EventsOn('calibration:progress', (raw: any) => {
      const id = String(raw?.deviceId ?? raw?.DeviceID ?? '');
      if (!id || !connected.value[id]) return;
      calibration.value[id] = {
        phase: Math.min(3, Math.max(0, Number(raw?.phase ?? raw?.Phase ?? 0) || 0)),
        offsetMs: Number(raw?.offsetMs ?? raw?.OffsetMs ?? 0) || 0,
        rttMs: Number(raw?.rttMs ?? raw?.RttMs ?? 0) || 0,
        updatedAt: Date.now(),
      };
    });

    discover();
  }

  onUnmounted(() => {
    if (tickTimer) window.clearInterval(tickTimer);
  });

  return {
    devices,
    connected,
    connecting,
    calibration,
    statusInfo,
    status,
    headerStatus,
    isScanning,
    connectedCount,
    connectedDevices,
    supportedFrames,
    frameAvailable,
    calibratingCount,
    allCalibrated,
    showSyncPanel,
    devicePhase,
    deviceCalibrating,
    deviceInfo,
    deviceStalled,
    syncSummary,
    setStatus,
    setStatusBackend,
    setErrorStatus,
    discover,
    connect,
    disconnect,
    setChannelRoute,
    initDevices,
  };
}
