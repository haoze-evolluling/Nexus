<script setup lang="ts">
import type { ConnRequest } from '../../types';
import Icons from '../common/Icons.vue';
import { t } from '../../i18n';

defineProps<{
  requests: ConnRequest[];
  remember: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:remember', val: boolean): void;
  (e: 'respond', allow: boolean): void;
}>();
</script>

<template>
  <div v-if="requests.length" class="modal-overlay">
    <div
      class="modal-card"
      role="dialog"
      aria-modal="true"
      :aria-label="t('modal.title')"
    >
      <div class="modal-header">
        <div class="modal-icon-badge">
          <Icons name="phone" :size="20" />
        </div>
        <div class="modal-title-group">
          <h3 class="modal-title">
            {{ t('modal.title') }}
            <span v-if="requests.length > 1" class="modal-count">
              {{ t('modal.morePending', { n: requests.length - 1 }) }}
            </span>
          </h3>
          <p class="modal-desc">
            {{ requests[0].host }} {{ t('modal.desc') }}
          </p>
        </div>
      </div>

      <div class="modal-body">
        <div class="device-highlight-box">
          <span class="device-name">{{ requests[0].name }}</span>
          <span class="device-ip">{{ requests[0].host }}</span>
        </div>

        <label class="remember-label">
          <input
            type="checkbox"
            :checked="remember"
            class="checkbox-input"
            @change="emit('update:remember', ($event.target as HTMLInputElement).checked)"
          />
          <span class="checkbox-text">{{ t('modal.remember') }}</span>
        </label>
      </div>

      <div class="modal-actions">
        <button
          type="button"
          class="btn-secondary deny-btn"
          @click="emit('respond', false)"
        >
          {{ t('modal.deny') }}
        </button>
        <button
          type="button"
          class="allow-btn"
          @click="emit('respond', true)"
        >
          <Icons name="check" :size="16" />
          <span>{{ t('modal.allow') }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(11, 15, 25, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 100;
  animation: fadeIn 0.2s ease-out;
}

.modal-card {
  width: 100%;
  max-width: 440px;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-xl);
  padding: 24px;
  box-shadow: var(--shadow-modal);
  animation: scaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.96) translateY(4px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 16px;
}

.modal-icon-badge {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.modal-title {
  font-size: var(--font-size-lg);
  font-weight: 750;
  color: var(--color-text-primary);
}

.modal-count {
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--color-primary);
}

.modal-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 20px;
}

.device-highlight-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background-color: var(--color-bg-surface-elevated);
  border: 1px solid var(--color-border-subtle);
}

.device-name {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-primary);
}

.device-ip {
  font-size: var(--font-size-xs);
  font-family: monospace;
  color: var(--color-text-muted);
}

.remember-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  cursor: pointer;
}

.checkbox-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.modal-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.deny-btn {
  min-width: 80px;
}

.allow-btn {
  min-width: 90px;
}
</style>
