package com.haoze.nexus.audio

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Handles mDNS NSD service discovery registration for the audio speaker receiver.
 */
class AudioNsdRegistrar(private val context: Context) {

    companion object {
        private const val TAG = "AudioNsdRegistrar"
    }

    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null

    fun register(settings: AudioSettings) {
        // onStartCommand 在每次前台服务被拉起时都会触发（例如发起连接前的
        // ensureReceiverRunning）；重复注册同名服务会与自身记录冲突导致广播异常。
        if (registration != null) return

        nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val friendly = DeviceIdentity.friendlyName(LocaleManager.wrap(context))
        val info = NsdServiceInfo().apply {
            serviceName = "Nexus-$friendly"
            serviceType = "_nexus._udp."
            port = NexusProtocol.port
            setAttribute("role", "speaker")
            setAttribute("device_id", settings.deviceId)
            setAttribute("codec", "opus")
            setAttribute("sample_rate", NexusProtocol.sampleRate.toString())
            setAttribute("channels", NexusProtocol.channels.toString())
            setAttribute("bitrate", (settings.initialBitrateKbps * 1000).toString())
            setAttribute("frame_ms", NexusProtocol.supportedFrameMilliseconds.joinToString(","))
            setAttribute("current_frame_ms", settings.frameMs.toString())
            setAttribute("settings_updated_at", settings.updatedAtMs.toString())
            setAttribute("settings_device_id", settings.deviceId)
        }

        registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {
                Log.i(TAG, "advertising ${i.serviceName}")
            }

            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {
                Log.e(TAG, "NSD registration failed: $e")
                registration = null
            }

            override fun onServiceUnregistered(i: NsdServiceInfo) {
                registration = null
            }

            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {
                Log.e(TAG, "NSD unregistration failed: $e")
            }
        }

        nsd?.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration)
    }

    fun unregister() {
        registration?.let {
            try {
                nsd?.unregisterService(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister NSD service: ${e.message}")
            }
        }
        registration = null
        nsd = null
    }
}
