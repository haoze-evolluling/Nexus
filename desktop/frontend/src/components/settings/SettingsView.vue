<script setup lang="ts">
import type { AuthorizedDevice, Identity, LanguagePref, Settings, Theme } from '../../types';
import Icons from '../common/Icons.vue';
import { language, setLanguage, t } from '../../i18n';

const props = defineProps<{
  settings: Settings;
  identity: Identity;
  authorizedDevices: AuthorizedDevice[];
  ntpServer: string;
  ntpResult: string;
  isTestingNtp: boolean;
  supportedFrames: number[];
  frameAvailable: boolean;
}>();

const emit = defineEmits<{
  (e: 'updateSettings', next: Partial<Settings>): void;
  (e: 'setTheme', theme: Theme): void;
  (e: 'updateNtpServer', val: string): void;
  (e: 'saveNtp'): void;
  (e: 'testNtp'): void;
  (e: 'removeAuthorized', device: AuthorizedDevice): void;
}>();

const bitrateOptions = [64000, 96000, 128000, 192000];
const frameOptions = [10, 20];
const themeOptions: Theme[] = ['light', 'dark', 'system'];
const languageOptions: LanguagePref[] = ['system', 'zh', 'en'];

function onServerInput(e: Event) {
  const target = e.target as HTMLInputElement;
  emit('updateNtpServer', target.value);
}

function onServerBlur() {
  emit('saveNtp');
}

function restoreDefaultNtp() {
  emit('updateNtpServer', 'ntp.aliyun.com');
  emit('saveNtp');
}
</script>

<template>
  <div class="settings-view">
    <div class="settings-head">
      <h2 class="settings-title">{{ t('header.settings') }}</h2>
      <p class="settings-subtitle">{{ t('settings.hint') }}</p>
    </div>

    <!-- Section 1: Audio Streaming Parameters -->
    <div class="settings-card">
      <div class="card-header">
        <Icons name="waveform" :size="18" />
        <h3 class="card-title">{{ t('settings.bitrate') }}</h3>
      </div>
      <div class="chips-grid">
        <button
          v-for="bitrate in bitrateOptions"
          :key="bitrate"
          type="button"
          class="chip-option"
          :class="{ selected: settings.bitrate === bitrate }"
          @click="emit('updateSettings', { bitrate })"
        >
          <span class="chip-main">{{ bitrate / 1000 }} kbps</span>
          <span v-if="bitrate === 128000" class="chip-tag">Recommended</span>
        </button>
      </div>

      <div class="card-divider"></div>

      <div class="card-header">
        <Icons name="activity" :size="18" />
        <h3 class="card-title">{{ t('settings.frame') }}</h3>
      </div>
      <div class="chips-grid">
        <button
          v-for="frame in frameOptions"
          :key="frame"
          type="button"
          class="chip-option"
          :class="{
            selected: settings.frameMs === frame,
            disabled: !supportedFrames.includes(frame),
          }"
          :disabled="!supportedFrames.includes(frame)"
          @click="emit('updateSettings', { frameMs: frame })"
        >
          <span class="chip-main">{{ frame }} ms</span>
          <span v-if="frame === 10" class="chip-tag">Low Latency</span>
        </button>
      </div>
      <p v-if="!frameAvailable" class="warning-banner">
        {{ t('settings.frameWarning') }}
      </p>
    </div>

    <!-- Section 2: Time Calibration (NTP) -->
    <div class="settings-card">
      <div class="card-header">
        <Icons name="clock" :size="18" />
        <h3 class="card-title">{{ t('settings.ntp') }}</h3>
      </div>
      <p class="field-hint">{{ t('settings.ntpHint') }}</p>

      <div class="ntp-input-row">
        <div class="input-wrapper">
          <input
            :value="ntpServer"
            class="text-input"
            type="text"
            inputmode="url"
            autocomplete="off"
            placeholder="ntp.aliyun.com"
            @input="onServerInput"
            @change="onServerBlur"
          />
        </div>
        <button
          type="button"
          class="btn-secondary"
          @click="restoreDefaultNtp"
        >
          {{ t('settings.ntpDefault') }}
        </button>
        <button
          type="button"
          :disabled="isTestingNtp"
          @click="emit('testNtp')"
        >
          <span v-if="isTestingNtp" class="spinner"></span>
          <span>{{ t('settings.ntpTest') }}</span>
        </button>
      </div>

      <p v-if="ntpResult" class="ntp-result-badge">
        <Icons name="info" :size="14" />
        <span>{{ ntpResult }}</span>
      </p>
    </div>

    <!-- Section 3: Appearance & Language -->
    <div class="settings-grid-cols">
      <!-- Theme Selection -->
      <div class="settings-card">
        <div class="card-header">
          <Icons name="sun" :size="18" />
          <h3 class="card-title">{{ t('settings.appearance') }}</h3>
        </div>
        <div class="segmented-col">
          <button
            v-for="theme in themeOptions"
            :key="theme"
            type="button"
            class="segmented-item"
            :class="{ active: settings.theme === theme }"
            @click="emit('setTheme', theme)"
          >
            <Icons
              :name="theme === 'light' ? 'sun' : theme === 'dark' ? 'moon' : 'monitor'"
              :size="16"
            />
            <span>{{ t('theme.' + theme) }}</span>
            <Icons
              v-if="settings.theme === theme"
              name="check"
              :size="16"
              class="active-check"
            />
          </button>
        </div>
      </div>

      <!-- Language Selection -->
      <div class="settings-card">
        <div class="card-header">
          <Icons name="radio" :size="18" />
          <h3 class="card-title">{{ t('settings.language') }}</h3>
        </div>
        <div class="segmented-col">
          <button
            v-for="lang in languageOptions"
            :key="lang"
            type="button"
            class="segmented-item"
            :class="{ active: language === lang }"
            @click="setLanguage(lang)"
          >
            <span>{{ t('language.' + lang) }}</span>
            <Icons
              v-if="language === lang"
              name="check"
              :size="16"
              class="active-check"
            />
          </button>
        </div>
      </div>
    </div>

    <!-- Section 4: Authorized Devices -->
    <div class="settings-card">
      <div class="card-header">
        <Icons name="shield" :size="18" />
        <h3 class="card-title">{{ t('settings.authorized') }}</h3>
      </div>
      <p class="field-hint">{{ t('settings.authorizedHint') }}</p>

      <div v-if="authorizedDevices.length" class="auth-list">
        <div
          v-for="device in authorizedDevices"
          :key="device.ID"
          class="auth-item"
        >
          <div class="auth-meta">
            <strong class="auth-name">{{ device.Name || t('device.unnamed') }}</strong>
            <span class="auth-id">{{ device.ID }}</span>
          </div>
          <button
            type="button"
            class="btn-danger remove-btn"
            @click="emit('removeAuthorized', device)"
          >
            <Icons name="trash" :size="14" />
            <span>{{ t('device.remove') }}</span>
          </button>
        </div>
      </div>

      <div v-else class="auth-empty">
        <p>{{ t('settings.authorizedEmpty') }}</p>
      </div>
    </div>

    <!-- Section 5: Local PC Identity -->
    <div class="settings-card readonly-card">
      <div class="card-header">
        <Icons name="pc" :size="18" />
        <h3 class="card-title">{{ t('settings.localInfo') }}</h3>
      </div>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">{{ t('info.name', { value: '' }).replace('：', '') }}</span>
          <span class="info-value">{{ identity.name || t('info.unnamed') }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('info.id', { value: '' }).replace('：', '') }}</span>
          <span class="info-value code">{{ identity.deviceId }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">Audio Codec</span>
          <span class="info-value">Opus (48 kHz)</span>
        </div>
        <div class="info-item">
          <span class="info-label">Channels</span>
          <span class="info-value">Stereo (2.0)</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.settings-title {
  font-size: var(--font-size-xl);
  font-weight: 750;
  color: var(--color-text-primary);
  letter-spacing: -0.4px;
  line-height: 1.25;
}

.settings-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.4;
}

.settings-card {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 22px 24px;
  box-shadow: var(--shadow-xs);
}

.card-divider {
  height: 1px;
  background-color: var(--color-border-subtle);
  margin: 24px 0 20px;
}

.settings-grid-cols {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-primary);
  margin-bottom: 14px;
}

.card-title {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-primary);
}

.chips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(136px, 1fr));
  gap: 12px;
}

.chip-option {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: auto;
  min-height: 58px;
  padding: 12px 14px;
  background-color: var(--color-bg-surface-elevated);
  border: 1.5px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: none;
}

.chip-option:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background-color: var(--color-bg-surface-hover);
}

.chip-option.selected {
  background-color: var(--color-primary-soft);
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.chip-option.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chip-main {
  font-size: var(--font-size-base);
  font-weight: 700;
}

.chip-tag {
  font-size: var(--font-size-2xs);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-top: 3px;
}

.chip-option.selected .chip-tag {
  color: var(--color-primary);
  opacity: 0.9;
}

.warning-banner {
  margin-top: 14px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background-color: var(--color-warning-soft);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  font-size: var(--font-size-xs);
  font-weight: 500;
  line-height: 1.45;
}

.field-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: 14px;
  line-height: 1.45;
}

.ntp-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.input-wrapper {
  flex: 1;
  min-width: 220px;
}

.text-input {
  width: 100%;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  border: 1.5px solid var(--color-border-subtle);
  background-color: var(--color-bg-surface-elevated);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  outline: none;
  transition: all var(--transition-fast);
}

.text-input:focus {
  border-color: var(--color-border-focus);
  box-shadow: 0 0 0 3px var(--color-primary-glow);
  background-color: var(--color-bg-surface);
}

.ntp-result-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  background-color: var(--color-primary-soft);
  border: 1px solid var(--color-primary-soft-border);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.segmented-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.segmented-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  height: 44px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  background-color: var(--color-bg-surface-elevated);
  border: 1.5px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 500;
  box-shadow: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.segmented-item:hover:not(.active) {
  border-color: var(--color-border-strong);
  color: var(--color-text-primary);
  background-color: var(--color-bg-surface-hover);
}

.segmented-item.active {
  background-color: var(--color-primary-soft);
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.active-check {
  margin-left: auto;
  color: var(--color-primary);
}

/* Authorized Devices */
.auth-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.auth-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background-color: var(--color-bg-surface-elevated);
  border: 1px solid var(--color-border-subtle);
}

.auth-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.auth-name {
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--color-text-primary);
}

.auth-id {
  font-size: var(--font-size-xs);
  font-family: monospace;
  color: var(--color-text-secondary);
}

.remove-btn {
  height: 30px;
  padding: 0 12px;
  font-size: var(--font-size-xs);
  flex-shrink: 0;
}

.auth-empty {
  padding: 24px;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  background-color: var(--color-bg-surface-elevated);
  border-radius: var(--radius-md);
  border: 1px dashed var(--color-border-subtle);
}

/* Local PC Identity */
.readonly-card {
  background-color: var(--color-bg-surface-elevated);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-label {
  font-size: var(--font-size-2xs);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--color-text-primary);
}

.info-value.code {
  font-family: monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  word-break: break-all;
}
</style>
