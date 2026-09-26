<script setup lang="ts">
import Icons from '../common/Icons.vue';
import { t } from '../../i18n';

defineProps<{
  activeTab: 'devices' | 'settings';
  headerStatus: string;
  connectedCount: number;
}>();

const emit = defineEmits<{
  (e: 'update:activeTab', tab: 'devices' | 'settings'): void;
}>();
</script>

<template>
  <header class="app-header">
    <div class="brand-group">
      <div class="brand-icon">
        <Icons name="logo" :size="24" />
      </div>
      <div class="brand-text">
        <h1 class="brand-title">Nexus</h1>
        <p class="brand-eyebrow">{{ t('header.eyebrow') }}</p>
      </div>
    </div>

    <div class="status-center">
      <div
        class="status-pill"
        :class="{ live: connectedCount > 0 }"
        :title="headerStatus"
      >
        <span class="status-dot">
          <span v-if="connectedCount > 0" class="dot-ping"></span>
        </span>
        <span class="status-text">{{ headerStatus }}</span>
      </div>
    </div>

    <nav class="nav-segmented" role="tablist">
      <button
        type="button"
        role="tab"
        :aria-selected="activeTab === 'devices'"
        class="nav-tab"
        :class="{ active: activeTab === 'devices' }"
        @click="emit('update:activeTab', 'devices')"
      >
        <Icons name="radio" :size="16" />
        <span>{{ t('header.devices') }}</span>
      </button>

      <button
        type="button"
        role="tab"
        :aria-selected="activeTab === 'settings'"
        class="nav-tab"
        :class="{ active: activeTab === 'settings' }"
        @click="emit('update:activeTab', 'settings')"
      >
        <Icons name="settings" :size="16" />
        <span>{{ t('header.settings') }}</span>
      </button>
    </nav>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 28px;
  background-color: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border-subtle);
  position: sticky;
  top: 0;
  z-index: 20;
  box-shadow: var(--shadow-xs);
  backdrop-filter: blur(8px);
}

.brand-group {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 180px;
  flex-shrink: 0;
}

.brand-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-primary);
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: var(--font-size-xl);
  font-weight: 750;
  letter-spacing: -0.5px;
  color: var(--color-text-primary);
  line-height: 1.15;
}

.brand-eyebrow {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background-color: var(--color-bg-surface-elevated);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 500;
  max-width: 440px;
  transition: all var(--transition-normal);
}

.status-pill.live {
  background-color: var(--color-primary-soft);
  border-color: var(--color-primary-soft-border);
  color: var(--color-primary);
  font-weight: 600;
}

.status-dot {
  position: relative;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-border-strong);
  flex-shrink: 0;
  transition: background-color var(--transition-normal);
}

.status-pill.live .status-dot {
  background-color: var(--color-primary);
}

.dot-ping {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background-color: var(--color-primary);
  opacity: 0.45;
  animation: pulse 1.8s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.status-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-segmented {
  display: inline-flex;
  padding: 3px;
  border-radius: var(--radius-lg);
  background-color: var(--color-bg-surface-elevated);
  border: 1px solid var(--color-border-subtle);
  gap: 3px;
  flex-shrink: 0;
}

.nav-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 32px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  white-space: nowrap;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid transparent;
  box-shadow: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.nav-tab:hover:not(.active) {
  color: var(--color-text-primary);
  background-color: var(--color-bg-surface);
}

.nav-tab:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.nav-tab.active {
  background-color: var(--color-primary);
  color: var(--color-primary-contrast);
  box-shadow: var(--shadow-xs);
}

@media (max-width: 760px) {
  .app-header {
    flex-wrap: wrap;
    padding: 12px 16px;
  }
  .status-center {
    order: 3;
    width: 100%;
    margin-top: 6px;
  }
}
</style>
