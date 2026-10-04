import { ref } from 'vue';
import type { ConnRequest } from '../types';
import { RespondConnection } from '../../wailsjs/go/main/App';
import { EventsOn } from '../../wailsjs/runtime/runtime';
import { t } from '../i18n';

export function useConnRequests(
  onSuccess?: (request: ConnRequest, allow: boolean) => void,
  onError?: (error: unknown) => void
) {
  const connRequests = ref<ConnRequest[]>([]);
  const rememberChoice = ref(true);

  async function respondConnection(allow: boolean) {
    const request = connRequests.value[0];
    if (!request) return;
    connRequests.value = connRequests.value.slice(1);
    try {
      await RespondConnection(request.requestId, allow, rememberChoice.value);
      if (allow) {
        onSuccess?.(request, true);
      }
    } catch (error) {
      onError?.(error);
    }
  }

  function initConnRequests() {
    EventsOn('conn:request', (raw: any) => {
      const request: ConnRequest = {
        requestId: String(raw?.requestId ?? raw?.RequestID ?? ''),
        deviceId: String(raw?.deviceId ?? raw?.DeviceID ?? ''),
        name: String(raw?.name ?? raw?.Name ?? '') || t('modal.unknownDevice'),
        host: String(raw?.host ?? raw?.Host ?? ''),
      };
      if (
        request.requestId &&
        !connRequests.value.some((item) => item.requestId === request.requestId)
      ) {
        connRequests.value = [...connRequests.value, request];
      }
      rememberChoice.value = true;
    });

    EventsOn('conn:cancelled', (requestId: unknown) => {
      connRequests.value = connRequests.value.filter(
        (item) => item.requestId !== String(requestId)
      );
    });
  }

  return {
    connRequests,
    rememberChoice,
    respondConnection,
    initConnRequests,
  };
}
