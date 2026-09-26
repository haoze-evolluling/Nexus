<script setup lang="ts">
import type { Calibration, ChannelRoute, Device, DeviceStatus } from '../../types';
import Icons from '../common/Icons.vue';
import DeviceCard from './DeviceCard.vue';
import SyncPanel from './SyncPanel.vue';
import { t } from '../../i18n';

defineProps<{
  devices: Device[];
  connected: Record<string, DeviceStatus>;
  connecting: Record<string, boolean>;
  calibration: Record<string, Calibration>;
  isScanning: boolean;
  connectedCount: number;
  connectedDevices: Device[];
  allCalibrated: boolean;
  showSyncPanel: boolean;
  devicePhase: (d: Device) => number;
  deviceInfo: (d: Device) => string;
  deviceStalled: (d: Device) => boolean;
}>();

const emit = defineEmits<{
  (e: 'discover'): void;
  (e: 'connect', device: Device): void;
  (e: 'disconnect', device: Device): void;
  (e: 'setChannelRoute', deviceId: string, route: ChannelRoute): void;
}>();
</script>

<template>
  <section class="device-section">
    <div class="section-head">
      <div class="head-text">
        <h2 class="section-title">{{ t('devices.title') }}</h2>
        <p class="section-hint">{{ t('devices.hint') }}</p>
      </div>

      <button
        type="button"
        class="btn-secondary rescan-btn"
        :disabled="isScanning"
        @click="emit('discover')"
      >
        <Icons
          name="refresh"
          :size="15"
          :class="{ 'spin-anim': isScanning }"
        />
        <span>{{ t('devices.rescan') }}</span>
      </button>
    </div>

    <!-- Multi-Device Sync Panel (NXAC) -->
    <Transition name="fold">
      <SyncPanel
        v-if="showSyncPanel"
        :all-calibrated="allCalibrated"
        :connected-devices="connectedDevices"
        :device-phase="devicePhase"
        :calibration="calibration"
      />
    </Transition>

    <!-- Available Receivers Cards -->
    <div v-if="devices.length" class="device-grid">
      <DeviceCard
        v-for="device in devices"
        :key="device.id"
        :device="device"
        :connected="Boolean(connected[device.id])"
        :connecting="Boolean(connecting[device.id])"
        :status="connected[device.id]"
        :phase="devicePhase(device)"
        :device-info="deviceInfo(device)"
        :device-stalled="deviceStalled(device)"
        @connect="emit('connect', $event)"
        @disconnect="emit('disconnect', $event)"
        @set-channel-route="(id, route) => emit('setChannelRoute', id, route)"
      />
    </div>

    <!-- Empty / Scanning State -->
    <div v-else class="empty-state">
      <div class="radar-box">
        <span class="radar-ring r1"></span>
        <span class="radar-ring r2"></span>
        <div class="radar-center">
          <Icons name="wifi" :size="24" />
        </div>
      </div>
      <p class="empty-title">{{ t('empty.scanning') }}</p>
      <p class="empty-hint">{{ t('empty.hint') }}</p>
    </div>

    <!-- Multi-device sync note when all calibrated -->
    <div
      v-if="connectedCount > 1 && !showSyncPanel && allCalibrated"
      class="sync-banner"
    >
      <Icons name="waveform" :size="16" />
      <span>{{ t('sync.note') }}</span>
    </div>
  </section>
</template>

<style scoped>
.device-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 2px;
}

.head-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: 750;
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
  line-height: 1.25;
}

.section-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.4;
}

.rescan-btn {
  height: 36px;
  padding: 0 16px;
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

.spin-anim {
  animation: spin 0.8s linear infinite;
}

.device-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Modern Radar Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
  background-color: var(--color-bg-surface);
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-xl);
  text-align: center;
  box-shadow: var(--shadow-xs);
}

.radar-box {
  position: relative;
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.radar-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1.5px solid var(--color-primary-soft-border);
  animation: pulse 2.4s ease-out infinite;
}

.radar-ring.r2 {
  inset: -14px;
  animation-delay: 0.8s;
}

.radar-center {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background-color: var(--color-primary-soft);
  border: 1px solid var(--color-primary-soft-border);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.empty-title {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.empty-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  max-width: 420px;
  line-height: 1.5;
}

.sync-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-radius: var(--radius-lg);
  background-color: var(--color-primary-soft);
  border: 1px solid var(--color-primary-soft-border);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 500;
  margin-top: 8px;
}
</style>
