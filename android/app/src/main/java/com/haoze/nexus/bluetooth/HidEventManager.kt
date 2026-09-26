package com.haoze.nexus.bluetooth

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Manages listeners and dispatches events for Bluetooth HID connection,
 * registration, send errors, and profile changes.
 */
class HidEventManager {
    private val connectionStateListeners = CopyOnWriteArraySet<(Boolean, String?) -> Unit>()
    private val registrationStateListeners = CopyOnWriteArraySet<(Boolean) -> Unit>()
    private val sendErrorListeners = CopyOnWriteArraySet<(String) -> Unit>()
    private val profileListeners = CopyOnWriteArraySet<(HidProfile) -> Unit>()

    fun addOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        connectionStateListeners.add(listener)
    }

    fun removeOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        connectionStateListeners.remove(listener)
    }

    fun addOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        registrationStateListeners.add(listener)
    }

    fun removeOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        registrationStateListeners.remove(listener)
    }

    fun addOnSendErrorListener(listener: (String) -> Unit) {
        sendErrorListeners.add(listener)
    }

    fun removeOnSendErrorListener(listener: (String) -> Unit) {
        sendErrorListeners.remove(listener)
    }

    fun addOnProfileChangedListener(listener: (HidProfile) -> Unit) {
        profileListeners.add(listener)
    }

    fun removeOnProfileChangedListener(listener: (HidProfile) -> Unit) {
        profileListeners.remove(listener)
    }

    fun notifyConnectionStateChanged(isConnected: Boolean, deviceName: String?) {
        connectionStateListeners.forEach { it(isConnected, deviceName) }
    }

    fun notifyRegistrationStateChanged(isRegistered: Boolean) {
        registrationStateListeners.forEach { it(isRegistered) }
    }

    fun notifySendError(message: String) {
        sendErrorListeners.forEach { it(message) }
    }

    fun notifyProfileChanged(profile: HidProfile) {
        profileListeners.forEach { it(profile) }
    }

    fun clear() {
        connectionStateListeners.clear()
        registrationStateListeners.clear()
        sendErrorListeners.clear()
        profileListeners.clear()
    }
}
