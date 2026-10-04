package com.haoze.nexus.bluetooth

import android.os.Handler
import android.util.Log

/**
 * Manages retry and reconnect attempts, backoff timing, and throttling for [BluetoothHidService].
 */
class HidReconnectManager {

    companion object {
        private const val TAG = "HidReconnectManager"
        const val MAX_REGISTRATION_RETRIES = 3
        const val MAX_RECONNECT_RETRIES = 5
        const val RECONNECT_BASE_DELAY_MS = 2_000L
        const val RECONNECT_MAX_DELAY_MS = 30_000L
        private const val RECONNECT_THROTTLE_MS = 2_000L
    }

    var registrationRetryCount: Int = 0
        private set

    var reconnectRetryCount: Int = 0
        private set

    @Volatile
    var lastReconnectAttempt: Long = 0
        private set

    fun resetRegistration() {
        registrationRetryCount = 0
    }

    fun resetReconnect() {
        reconnectRetryCount = 0
        lastReconnectAttempt = 0
    }

    fun decrementReconnectRetryCount() {
        reconnectRetryCount = maxOf(0, reconnectRetryCount - 1)
    }

    fun scheduleRegistrationRetry(
        handler: Handler,
        onRetry: () -> Unit,
        onExhausted: () -> Unit
    ): Boolean {
        if (registrationRetryCount >= MAX_REGISTRATION_RETRIES) {
            Log.w(TAG, "HID app registration failed after retries")
            onExhausted()
            return false
        }
        registrationRetryCount++
        Log.d(TAG, "Retrying HID app registration ($registrationRetryCount/$MAX_REGISTRATION_RETRIES)")
        handler.postDelayed(onRetry, 1000L * registrationRetryCount)
        return true
    }

    fun scheduleReconnect(
        handler: Handler,
        address: String,
        onReconnect: () -> Unit
    ): Boolean {
        if (reconnectRetryCount >= MAX_RECONNECT_RETRIES) {
            Log.w(TAG, "Reconnect retries exhausted ($MAX_RECONNECT_RETRIES) for $address")
            return false
        }

        // Exponential backoff: 2s, 4s, 8s, 16s, 30s (capped)
        val delay = minOf(
            RECONNECT_BASE_DELAY_MS * (1L shl reconnectRetryCount),
            RECONNECT_MAX_DELAY_MS
        )
        reconnectRetryCount++
        Log.d(TAG, "Scheduling reconnect attempt $reconnectRetryCount/$MAX_RECONNECT_RETRIES to $address in ${delay}ms")
        handler.postDelayed(onReconnect, delay)
        return true
    }

    /**
     * Checks if enough time has elapsed since the last reconnect attempt.
     * Updates [lastReconnectAttempt] if permitted.
     */
    fun checkAndRecordReconnectAttempt(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastReconnectAttempt < RECONNECT_THROTTLE_MS) return false
        lastReconnectAttempt = now
        return true
    }
}
