package com.haoze.nexus.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.os.Build
import android.util.Log

/**
 * Handles reflection and policy calls on [BluetoothAdapter] and [BluetoothHidDevice].
 */
object HidDevicePolicyHelper {

    private const val TAG = "HidDevicePolicyHelper"

    /**
     * Attempts to set the Bluetooth adapter discoverable mode using reflection on setScanMode.
     */
    fun setDiscoverable(adapter: BluetoothAdapter?) {
        try {
            val method = adapter?.javaClass?.getMethod(
                "setScanMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            val result = method?.invoke(adapter, 23, 300)
            Log.d(TAG, "setScanMode result: $result")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set discoverable mode: ${e.message}")
        }
    }

    /**
     * Sets connection policy to CONNECTION_POLICY_ALLOWED (1) on Android 13 (Tiramisu) or above.
     */
    fun setConnectionPolicy(hidDevice: BluetoothHidDevice, device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val method = hidDevice.javaClass.getMethod(
                    "setConnectionPolicy",
                    BluetoothDevice::class.java,
                    Int::class.javaPrimitiveType
                )
                method.invoke(hidDevice, device, 1) // CONNECTION_POLICY_ALLOWED
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set connection policy: ${e.message}")
            }
        }
    }

    /**
     * Invokes sendReply on BluetoothHidDevice to respond to onGetReport requests.
     */
    fun sendReportReply(
        hidDevice: BluetoothHidDevice,
        device: BluetoothDevice,
        type: Byte,
        id: Byte,
        data: ByteArray = ByteArray(8)
    ) {
        try {
            val method = hidDevice.javaClass.getMethod(
                "sendReply",
                BluetoothDevice::class.java,
                Byte::class.javaPrimitiveType,
                Byte::class.javaPrimitiveType,
                ByteArray::class.java
            )
            method.invoke(hidDevice, device, type, id, data)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send GET_REPORT reply: ${e.message}")
        }
    }
}
