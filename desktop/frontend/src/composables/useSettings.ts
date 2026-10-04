import { ref, watch } from 'vue';
import type { AuthorizedDevice, Identity, Settings, Theme } from '../types';
import {
  GetIdentity,
  GetNTPSettings,
  ListAuthorizedDevices,
  RemoveAuthorizedDevice,
  SaveLocalSettings,
  SaveNTPServer,
  TestNTPServer,
} from '../../wailsjs/go/main/App';
import {
  WindowSetDarkTheme,
  WindowSetLightTheme,
  WindowSetSystemDefaultTheme,
} from '../../wailsjs/runtime/runtime';
import { t } from '../i18n';

const settingsKey = 'nexus.desktop.settings.v2';
const deviceIdKey = 'nexus.desktop.device_id';

const deviceId = localStorage.getItem(deviceIdKey) ?? crypto.randomUUID();
localStorage.setItem(deviceIdKey, deviceId);

const defaults: Settings = {
  bitrate: 128000,
  frameMs: 10,
  theme: 'system',
  updatedAtMs: 0,
  deviceId,
};

function normalizeSettings(raw: unknown): Settings {
  const value = raw as Partial<Settings> | null;
  return {
    bitrate: [64000, 96000, 128000, 192000].includes(value?.bitrate ?? 0)
      ? value!.bitrate!
      : defaults.bitrate,
    frameMs: value?.frameMs === 20 ? 20 : 10,
    theme:
      value?.theme === 'light' || value?.theme === 'dark' || value?.theme === 'system'
        ? value.theme
        : defaults.theme,
    updatedAtMs: Number(value?.updatedAtMs) || 0,
    deviceId:
      typeof value?.deviceId === 'string' && value.deviceId
        ? value.deviceId
        : deviceId,
  };
}

export function useSettings() {
  const settings = ref<Settings>({ ...defaults });
  const identity = ref<Identity>({ deviceId: '', name: '' });
  const authorizedDevices = ref<AuthorizedDevice[]>([]);
  const ntpServer = ref('ntp.aliyun.com');
  const ntpResult = ref('');
  const isTestingNtp = ref(false);

  function applyTheme(theme: Theme) {
    document.documentElement.dataset.theme = theme;
    if (theme === 'dark') {
      WindowSetDarkTheme();
    } else if (theme === 'light') {
      WindowSetLightTheme();
    } else {
      WindowSetSystemDefaultTheme();
    }
  }

  function touch(next: Partial<Settings>) {
    settings.value = {
      ...settings.value,
      ...next,
      updatedAtMs: Date.now(),
      deviceId,
    };
  }

  function setTheme(theme: Theme) {
    touch({ theme });
    applyTheme(theme);
  }

  async function refreshIdentity() {
    try {
      const value: any = await GetIdentity();
      identity.value = {
        deviceId: String(value?.deviceId ?? value?.DeviceID ?? ''),
        name: String(value?.name ?? value?.Name ?? ''),
      };
    } catch {
      identity.value = { deviceId: '', name: '' };
    }
  }

  async function refreshAuthorized() {
    try {
      const list: any = await ListAuthorizedDevices();
      authorizedDevices.value = (Array.isArray(list) ? list : []).map((raw: any) => ({
        ID: String(raw?.ID ?? raw?.id ?? ''),
        Name: String(raw?.Name ?? raw?.name ?? ''),
        AddedAtMs: Number(raw?.AddedAtMs ?? raw?.addedAtMs) || 0,
      }));
    } catch {
      authorizedDevices.value = [];
    }
  }

  async function removeAuthorized(device: AuthorizedDevice): Promise<boolean> {
    try {
      await RemoveAuthorizedDevice(device.ID);
      await refreshAuthorized();
      return true;
    } catch {
      return false;
    }
  }

  async function refreshNtp() {
    try {
      const value: any = await GetNTPSettings();
      ntpServer.value = String(value?.server ?? value?.Server ?? 'ntp.aliyun.com');
    } catch {
      ntpServer.value = 'ntp.aliyun.com';
    }
  }

  async function saveNtp(): Promise<boolean> {
    const trimmed = ntpServer.value.trim() || 'ntp.aliyun.com';
    try {
      await SaveNTPServer(trimmed);
      ntpServer.value = trimmed;
      ntpResult.value = t('settings.ntpSaved');
      return true;
    } catch {
      ntpResult.value = t('settings.ntpInvalid');
      return false;
    }
  }

  async function testNtp() {
    isTestingNtp.value = true;
    ntpResult.value = '';
    await saveNtp();
    try {
      const value: any = await TestNTPServer();
      const reachable = Boolean(value?.reachable ?? value?.Reachable);
      if (reachable) {
        const offset = Math.abs(Number(value?.offsetMs ?? value?.OffsetMs) || 0);
        ntpResult.value = t('settings.ntpOffset', { offset });
      } else {
        ntpResult.value = t('settings.ntpUnavailable');
      }
    } catch {
      ntpResult.value = t('settings.ntpUnavailable');
    } finally {
      isTestingNtp.value = false;
    }
  }

  function initSettings() {
    try {
      settings.value = normalizeSettings(
        JSON.parse(localStorage.getItem(settingsKey) ?? 'null')
      );
    } catch {
      settings.value = { ...defaults };
    }
    applyTheme(settings.value.theme);
    SaveLocalSettings(settings.value.bitrate, settings.value.frameMs).catch(() => {});
  }

  watch(
    settings,
    (next) => {
      localStorage.setItem(settingsKey, JSON.stringify(next));
      applyTheme(next.theme);
      SaveLocalSettings(next.bitrate, next.frameMs).catch(() => {});
    },
    { deep: true }
  );

  return {
    settings,
    identity,
    authorizedDevices,
    ntpServer,
    ntpResult,
    isTestingNtp,
    initSettings,
    touch,
    setTheme,
    applyTheme,
    refreshIdentity,
    refreshAuthorized,
    removeAuthorized,
    refreshNtp,
    saveNtp,
    testNtp,
  };
}
