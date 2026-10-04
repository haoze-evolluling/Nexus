<script setup lang="ts">
import { computed } from 'vue';
import type { Calibration, Device } from '../../types';
import { t } from '../../i18n';

const props = defineProps<{
  allCalibrated: boolean;
  connectedDevices: Device[];
  devicePhase: (device: Device) => number;
  calibration: Record<string, Calibration>;
}>();

const phaseHints = computed(() => [
  t('phaseHint.detect'),
  t('phaseHint.calculate'),
  t('phaseHint.sync'),
  t('phaseHint.done'),
]);
</script>

<template>
  <div class="sync-panel" :class="{ done: allCalibrated }">
    <div class="sync-head">
      <div class="pc-chip">
        <span>PC</span>
      </div>

      <div class="sync-title">
        <h3>{{ allCalibrated ? t('sync.doneTitle') : t('sync.busyTitle') }}</h3>
        <p>{{ allCalibrated ? t('sync.doneHint') : t('sync.busyHint') }}</p>
      </div>

      <div class="wave-container" aria-hidden="true">
        <span class="wave-bars">
          <i
            v-for="n in 12"
            :key="n"
            :style="{ animationDelay: `${n * 75}ms` }"
          ></i>
        </span>
      </div>
    </div>

    <div class="node-rows">
      <div
        v-for="(device, index) in connectedDevices"
        :key="device.id"
        class="node-row"
      >
        <div class="link-line" aria-hidden="true">
          <i
            class="link-pulse"
            :style="{ animationDelay: `${index * 260}ms` }"
          ></i>
        </div>

        <div
          class="node-chip"
          :class="`phase-${devicePhase(device)}`"
          :title="device.name"
        >
          {{ (device.name.trim()[0] || 'S').toUpperCase() }}
        </div>

        <div class="node-meta">
          <div class="node-name-row">
            <strong class="node-name">{{ device.name }}</strong>
            <span
              v-if="devicePhase(device) >= 2 && calibration[device.id]"
              class="node-stats-badge"
            >
              {{
                t('calib.stats', {
                  offset: Math.abs(calibration[device.id].offsetMs),
                  rtt: calibration[device.id].rttMs,
                })
              }}
            </span>
          </div>

          <span class="node-hint" :class="{ ok: devicePhase(device) >= 3 }">
            {{ phaseHints[devicePhase(device)] }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sync-panel {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-primary-soft-border);
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-bg-surface));
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.sync-panel.done {
  border-color: var(--color-success-border);
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-bg-surface));
}

.sync-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pc-chip {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: var(--font-size-base);
  box-shadow: var(--shadow-primary);
  flex-shrink: 0;
}

.sync-title {
  flex: 1;
  min-width: 0;
}

.sync-title h3 {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 3px;
}

.sync-panel.done .sync-title h3 {
  color: var(--color-primary);
}

.sync-title p {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.wave-container {
  display: flex;
  align-items: center;
  padding: 0 4px;
}

.wave-bars {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 24px;
}

.wave-bars i {
  width: 3px;
  height: 8px;
  border-radius: 2px;
  background-color: var(--color-primary);
  opacity: 0.85;
  animation: wavebar 1s ease-in-out infinite;
}

.node-rows {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--color-primary-soft-border);
}

.node-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.link-line {
  position: relative;
  width: 42px;
  height: 2px;
  flex-shrink: 0;
  background: linear-gradient(90deg, var(--color-primary-soft-border), var(--color-primary));
  border-radius: 1px;
}

.link-pulse {
  position: absolute;
  top: 50%;
  left: 0;
  width: 6px;
  height: 6px;
  margin: -3px 0 0 -3px;
  border-radius: 50%;
  background-color: var(--color-primary);
  box-shadow: 0 0 8px var(--color-primary);
  animation: travel 1.4s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.node-chip {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: var(--color-text-secondary);
  background-color: var(--color-bg-surface-elevated);
  border: 1.5px solid var(--color-border-subtle);
  flex-shrink: 0;
  transition: all var(--transition-normal);
}

.node-chip.phase-1 {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background-color: var(--color-primary-soft);
}

.node-chip.phase-2 {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background-color: var(--color-primary-soft);
  box-shadow: 0 0 0 3px var(--color-primary-glow);
}

.node-chip.phase-3 {
  background-color: var(--color-primary);
  color: #ffffff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-glow);
}

.node-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.node-name {
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--color-text-primary);
}

.node-stats-badge {
  font-size: var(--font-size-2xs);
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
}

.node-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.node-hint.ok {
  color: var(--color-primary);
  font-weight: 600;
}
</style>
