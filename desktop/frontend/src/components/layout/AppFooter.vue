<script setup lang="ts">
import Icons from '../common/Icons.vue';
import { t } from '../../i18n';
import type { Settings } from '../../types';

defineProps<{
  settings: Settings;
  connectedCount: number;
}>();
</script>

<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div v-if="connectedCount > 0" class="footer-badge connected">
        <Icons name="waveform" :size="13" />
        <span>{{ t('footer.connectedPrefix', { n: connectedCount }) }}</span>
      </div>

      <div class="footer-item">
        <span class="metric-val">{{ settings.bitrate / 1000 }} kbps</span>
      </div>

      <span class="divider">·</span>

      <div class="footer-item">
        <span class="metric-val">{{ settings.frameMs }} ms</span>
      </div>

      <span class="divider">·</span>

      <div class="footer-item specs">
        <span>{{ t('footer.tail') }}</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  margin-top: auto;
  padding: 12px 24px;
  background-color: var(--color-bg-surface);
  border-top: 1px solid var(--color-border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.footer-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.footer-badge.connected {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

.footer-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.metric-val {
  font-weight: 600;
  color: var(--color-text-secondary);
}

.divider {
  color: var(--color-border-strong);
}

.specs {
  color: var(--color-text-muted);
}
</style>
