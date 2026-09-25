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
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 4px;
}

.head-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: 750;
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
}

.section-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.rescan-btn {
  padding: 7px 14px;
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

.spin-anim {
  animation: spin 0.8s linear infinite;
}

.device-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Modern Radar Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background-color: var(--color-bg-surface);
  border: 1px dashed var(--color-border-subtle);
  border-radius: var(--radius-xl);
  text-align: center;
}

.radar-box {
  position: relative;
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.radar-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1.5px solid var(--color-primary-soft-border);
  animation: pulse 2.4s ease-out infinite;
}

.radar-ring.r2 {
  inset: -12px;
  animation-delay: 0.8s;
}

.radar-center {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.empty-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.empty-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  max-width: 360px;
}

.sync-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  background-color: var(--color-primary-soft);
  border: 1px solid var(--color-primary-soft-border);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: 500;
  margin-top: 8px;
}
</style>
