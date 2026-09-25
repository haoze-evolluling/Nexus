<script setup lang="ts">
import { computed } from 'vue';
import type { ChannelRoute, Device, DeviceStatus } from '../../types';
import Icons from '../common/Icons.vue';
import { t } from '../../i18n';

const props = defineProps<{
  device: Device;
  connected: boolean;
  connecting: boolean;
  status?: DeviceStatus;
  phase: number;
  deviceInfo: string;
  deviceStalled: boolean;
}>();

const emit = defineEmits<{
  (e: 'connect', device: Device): void;
  (e: 'disconnect', device: Device): void;
  (e: 'setChannelRoute', deviceId: string, route: ChannelRoute): void;
}>();

const currentRoute = computed<ChannelRoute>(() => {
  return props.status?.channelRoute || 'stereo';
});

const phaseLabels = computed(() => [
  t('phase.detect'),
  t('phase.calculate'),
  t('phase.sync'),
  t('phase.done'),
]);

const phaseHints = computed(() => [
  t('phaseHint.detect'),
  t('phaseHint.calculate'),
  t('phaseHint.sync'),
  t('phaseHint.done'),
]);
</script>

<template>
  <article class="device-card" :class="{ live: connected }">
    <div class="card-main">
      <div class="device-avatar" :class="{ live: connected }">
        <Icons :name="connected ? 'waveform' : 'phone'" :size="20" />
      </div>

      <div class="device-info">
        <div class="device-header-line">
          <h3 class="device-name">{{ device.name }}</h3>
          <span
            v-if="deviceInfo"
            class="live-badge"
            :class="{ warn: deviceStalled }"
          >
            {{ deviceInfo }}
          </span>
        </div>

        <div class="device-meta">
          <span class="host-text">{{ device.host }}:{{ device.port }}</span>
          <span class="meta-dot">·</span>
          <span class="frame-tag">
            {{ t('device.supports', { frames: device.supportedFrameMs.join('/') }) }}
          </span>
        </div>

        <!-- Channel Route Selector (when connected) -->
        <div v-if="connected" class="channel-section">
          <span class="channel-label">{{ t('channel.label') }}</span>
          <div class="channel-group" role="group">
            <button
              type="button"
              class="channel-btn"
              :class="{ active: currentRoute === 'left' }"
              @click="emit('setChannelRoute', device.id, 'left')"
            >
              {{ t('channel.left') }}
            </button>
            <button
              type="button"
              class="channel-btn"
              :class="{ active: currentRoute === 'stereo' }"
              @click="emit('setChannelRoute', device.id, 'stereo')"
            >
              {{ t('channel.stereo') }}
            </button>
            <button
              type="button"
              class="channel-btn"
              :class="{ active: currentRoute === 'right' }"
              @click="emit('setChannelRoute', device.id, 'right')"
            >
              {{ t('channel.right') }}
            </button>
          </div>
        </div>

        <!-- In-card Calibration Progress (when connected and phase < 3) -->
        <Transition name="fade">
          <div v-if="connected && phase < 3" class="calib-section">
            <div class="calib-wave" aria-hidden="true">
              <i
                v-for="n in 8"
                :key="n"
                :style="{ animationDelay: `${n * 80}ms` }"
              ></i>
            </div>
            <ol class="calib-steps">
              <li
                v-for="(label, i) in phaseLabels"
                :key="label"
                :class="{ active: i === phase, done: i < phase }"
              >
                {{ label }}
              </li>
            </ol>
            <span class="calib-hint">{{ phaseHints[phase] }}</span>
          </div>
        </Transition>
      </div>

      <!-- Action Button -->
      <div class="device-actions">
        <button
          v-if="connected"
          type="button"
          class="btn-secondary disconnect-btn"
          @click="emit('disconnect', device)"
        >
          {{ t('device.disconnect') }}
        </button>

        <button
          v-else-if="connecting"
          type="button"
          class="btn-secondary"
          disabled
        >
          <span class="spinner"></span>
          <span>{{ t('device.waiting') }}</span>
        </button>

        <button
          v-else
          type="button"
          class="connect-btn"
          @click="emit('connect', device)"
        >
          <Icons name="radio" :size="15" />
          <span>{{ t('device.connect') }}</span>
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.device-card {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-shadow: var(--shadow-xs);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.device-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-sm);
}

.device-card.live {
  border-color: var(--color-primary-soft-border);
  background-color: var(--color-bg-surface);
  box-shadow: var(--shadow-sm);
}

.device-card.live::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background-color: var(--color-primary);
}

.card-main {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.device-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background-color: var(--color-bg-surface-elevated);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--transition-normal);
}

.device-avatar.live {
  background-color: var(--color-primary-soft);
  border-color: var(--color-primary-soft-border);
  color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-glow);
}

.device-info {
  flex: 1;
  min-width: 0;
}

.device-header-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.device-name {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.25;
}

.live-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 600;
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  border: 1px solid var(--color-primary-soft-border);
}

.live-badge.warn {
  background-color: var(--color-warning-soft);
  color: var(--color-warning);
  border-color: var(--color-warning-border);
}

.device-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  flex-wrap: wrap;
}

.host-text {
  font-family: monospace;
}

.meta-dot {
  color: var(--color-border-strong);
}

.frame-tag {
  color: var(--color-text-muted);
}

.channel-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-border-subtle);
  flex-wrap: wrap;
}

.channel-label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-text-secondary);
}

.channel-group {
  display: inline-flex;
  padding: 2px;
  background-color: var(--color-bg-surface-elevated);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.channel-btn {
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 500;
  background: transparent;
  color: var(--color-text-secondary);
  border: none;
  box-shadow: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.channel-btn:hover:not(.active) {
  color: var(--color-text-primary);
  background-color: var(--color-bg-surface);
}

.channel-btn.active {
  background-color: var(--color-primary);
  color: #ffffff;
  font-weight: 600;
  box-shadow: var(--shadow-xs);
}

.calib-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.calib-wave {
  display: inline-flex;
  align-items: center;
  gap: 2.5px;
  height: 16px;
}

.calib-wave i {
  width: 2.5px;
  height: 6px;
  border-radius: 1px;
  background-color: var(--color-primary);
  animation: wavebar 0.9s ease-in-out infinite;
}

.calib-steps {
  display: flex;
  gap: 4px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.calib-steps li {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: var(--radius-full);
  background-color: var(--color-bg-surface-elevated);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border-subtle);
  transition: all var(--transition-normal);
}

.calib-steps li.done {
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  border-color: var(--color-primary-soft-border);
}

.calib-steps li.active {
  background-color: var(--color-primary);
  color: #ffffff;
  border-color: var(--color-primary);
  animation: breathe 1.2s ease-in-out infinite;
}

.calib-hint {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.device-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.connect-btn {
  min-width: 88px;
}

.disconnect-btn {
  min-width: 88px;
}
</style>
