<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useSettings } from './composables/useSettings';
import { useDevices } from './composables/useDevices';
import { useConnRequests } from './composables/useConnRequests';
import AppHeader from './components/layout/AppHeader.vue';
import AppFooter from './components/layout/AppFooter.vue';
import DeviceList from './components/devices/DeviceList.vue';
import SettingsView from './components/settings/SettingsView.vue';
import ConnRequestModal from './components/modals/ConnRequestModal.vue';
import { locale } from './i18n';

const activeTab = ref<'devices' | 'settings'>('devices');

// Settings management
const {
  settings,
  identity,
  authorizedDevices,
  ntpServer,
  ntpResult,
  isTestingNtp,
  initSettings,
  touch,
  setTheme,
  refreshIdentity,
  refreshAuthorized,
  removeAuthorized,
  refreshNtp,
  saveNtp,
  testNtp,
} = useSettings();

// Device and stream management
const {
  devices,
  connected,
  connecting,
  calibration,
  headerStatus,
  isScanning,
  connectedCount,
  connectedDevices,
  supportedFrames,
  frameAvailable,
  allCalibrated,
  showSyncPanel,
  devicePhase,
  deviceInfo,
  deviceStalled,
  setStatus,
  setErrorStatus,
  discover,
  connect,
  disconnect,
  setChannelRoute,
  initDevices,
} = useDevices(
  () => settings.value,
  (newerDevice) => {
    touch({
      bitrate: newerDevice.bitrate,
      frameMs: newerDevice.frameMs,
      updatedAtMs: newerDevice.updatedAtMs,
      deviceId: newerDevice.settingsDeviceId || settings.value.deviceId,
    });
  }
);

// Inbound connection request handling
const {
  connRequests,
  rememberChoice,
  respondConnection,
  initConnRequests,
} = useConnRequests(
  (req, allow) => {
    if (allow) setStatus('status.allowed', { name: req.name });
  },
  (err) => {
    setErrorStatus('status.respondFailed', err);
  }
);

watch(activeTab, (tab) => {
  if (tab === 'settings') {
    refreshAuthorized();
    refreshNtp();
    refreshIdentity();
  }
});

watch(
  locale,
  (l) => {
    document.documentElement.lang = l === 'zh' ? 'zh-CN' : 'en';
  },
  { immediate: true }
);

onMounted(() => {
  initSettings();
  initDevices();
  initConnRequests();
});
</script>

<template>
  <div class="nexus-desktop-app">
    <AppHeader
      :active-tab="activeTab"
      :header-status="headerStatus"
      :connected-count="connectedCount"
      @update:active-tab="activeTab = $event"
    />

    <main class="app-main-content">
      <div class="content-container">
        <Transition name="fade" mode="out-in">
          <DeviceList
            v-if="activeTab === 'devices'"
            key="devices"
            :devices="devices"
            :connected="connected"
            :connecting="connecting"
            :calibration="calibration"
            :is-scanning="isScanning"
            :connected-count="connectedCount"
            :connected-devices="connectedDevices"
            :all-calibrated="allCalibrated"
            :show-sync-panel="showSyncPanel"
            :device-phase="devicePhase"
            :device-info="deviceInfo"
            :device-stalled="deviceStalled"
            @discover="discover"
            @connect="connect"
            @disconnect="disconnect"
            @set-channel-route="setChannelRoute"
          />

          <SettingsView
            v-else
            key="settings"
            :settings="settings"
            :identity="identity"
            :authorized-devices="authorizedDevices"
            :ntp-server="ntpServer"
            :ntp-result="ntpResult"
            :is-testing-ntp="isTestingNtp"
            :supported-frames="supportedFrames"
            :frame-available="frameAvailable"
            @update-settings="touch"
            @set-theme="setTheme"
            @update-ntp-server="ntpServer = $event"
            @save-ntp="saveNtp"
            @test-ntp="testNtp"
            @remove-authorized="removeAuthorized"
          />
        </Transition>
      </div>
    </main>

    <AppFooter
      :settings="settings"
      :connected-count="connectedCount"
    />

    <ConnRequestModal
      :requests="connRequests"
      :remember="rememberChoice"
      @update:remember="rememberChoice = $event"
      @respond="respondConnection"
    />
  </div>
</template>

<style scoped>
.nexus-desktop-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-canvas);
}

.app-main-content {
  flex: 1;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.content-container {
  width: 100%;
  max-width: 860px;
}

@media (max-width: 640px) {
  .app-main-content {
    padding: 16px;
  }
}
</style>
