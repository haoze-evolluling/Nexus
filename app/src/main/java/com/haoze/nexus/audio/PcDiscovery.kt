package com.haoze.nexus.audio

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 局域网内一台可连接的 Nexus 电脑。 */
data class PcDevice(
    val deviceId: String,
    val name: String,
    val host: String,
    val port: Int,
    val seenAtMs: Long = System.currentTimeMillis(),
)

/** 局域网内可用于 Nexus 时钟校准的另一台 Android 设备。 */
data class AndroidDevice(
    val deviceId: String,
    val name: String,
    val host: String,
    val port: Int,
    val seenAtMs: Long = System.currentTimeMillis(),
)

/**
 * 浏览局域网中的 Nexus 电脑（role=pc），通过 StateFlow 发布设备列表。
 * NsdManager 同一时刻只允许一个 resolve，因此用队列串行解析。
 */
class PcDiscovery(context: Context) {
    private companion object {
        const val TAG = "NexusPcDiscovery"
        const val SERVICE_TYPE = "_nexus._udp."
    }

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _devices = MutableStateFlow<List<PcDevice>>(emptyList())
    val devices: StateFlow<List<PcDevice>> = _devices
    private val _androidDevices = MutableStateFlow<List<AndroidDevice>>(emptyList())
    val androidDevices: StateFlow<List<AndroidDevice>> = _androidDevices

    private var listener: NsdManager.DiscoveryListener? = null
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    fun start() {
        if (listener != null) return
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "discovery start failed: $errorCode")
                listener = null
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "discovery stop failed: $errorCode")
                listener = null
            }
            override fun onDiscoveryStopped(serviceType: String) { listener = null }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val id = serviceInfo.attributes?.get("device_id")?.decodeToString()
            if (id != null) {
                upsert { list -> list.filterNot { it.deviceId == id } }
                _androidDevices.value = _androidDevices.value.filterNot { it.deviceId == id }
            }
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.contains("nexus") && !serviceInfo.serviceType.contains("steamvoice")) return
                enqueueResolve(serviceInfo)
            }
        }
        listener = discoveryListener
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
    }

    private fun enqueueResolve(info: NsdServiceInfo) {
        synchronized(resolveQueue) {
            resolveQueue.addLast(info)
            if (resolving) return
            resolving = true
        }
        // NSD callbacks share one thread on some builds; never block it.
        kotlin.concurrent.thread(name = "nexus-nsd-resolve") { drainResolveQueue() }
    }

    private fun drainResolveQueue() {
        while (true) {
            val next = synchronized(resolveQueue) {
                if (resolveQueue.isEmpty()) {
                    resolving = false
                    return
                }
                resolveQueue.removeFirst()
            }
            val latch = java.util.concurrent.CountDownLatch(1)
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "resolve failed for ${info.serviceName}: $errorCode")
                    latch.countDown()
                }
                override fun onServiceResolved(info: NsdServiceInfo) {
                    onPcResolved(info)
                    latch.countDown()
                }
            })
            latch.await()
        }
    }

    private fun onPcResolved(info: NsdServiceInfo) {
        val attrs = info.attributes ?: return
        val role = attrs["role"]?.decodeToString() ?: return
        val deviceId = attrs["device_id"]?.decodeToString() ?: return
        val host = resolveHost(info) ?: return
        val name = info.serviceName.removePrefix("Nexus-").removePrefix("SteamVoice-").ifBlank { deviceId.take(8) }
        if (role == "speaker") {
            val device = AndroidDevice(deviceId, name, host, info.port)
            _androidDevices.value = (_androidDevices.value.filterNot { it.deviceId == deviceId } + device).sortedBy { it.name.lowercase() }
            return
        }
        if (role != "pc") return
        val device = PcDevice(deviceId = deviceId, name = name, host = host, port = info.port)
        upsert { list -> (list.filterNot { it.deviceId == deviceId } + device).sortedBy { it.name.lowercase() } }
    }

    private fun resolveHost(info: NsdServiceInfo): String? {
        if (Build.VERSION.SDK_INT >= 34) {
            val nonLoopback = info.hostAddresses.filterNot { it.isLoopbackAddress }
            val ipv4List = nonLoopback.filterIsInstance<Inet4Address>()
            if (ipv4List.isNotEmpty()) {
                val best = findBestSubnetMatch(ipv4List) ?: ipv4List.first()
                return best.hostAddress
            }
            return nonLoopback.firstOrNull()?.hostAddress
        }
        @Suppress("DEPRECATION")
        val host = info.host
        if (host is Inet4Address && !host.isLoopbackAddress) {
            return host.hostAddress
        }
        val candidateHost = host?.hostAddress ?: info.serviceName
        runCatching {
            val all = InetAddress.getAllByName(candidateHost).filterNot { it.isLoopbackAddress }
            val ipv4List = all.filterIsInstance<Inet4Address>()
            if (ipv4List.isNotEmpty()) {
                val best = findBestSubnetMatch(ipv4List) ?: ipv4List.first()
                return best.hostAddress
            }
        }
        return host?.hostAddress
    }

    private fun findBestSubnetMatch(candidates: List<Inet4Address>): Inet4Address? {
        val localIps = runCatching {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.filter { !it.isLoopback && it.isUp }
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.filterIsInstance<Inet4Address>()
                ?.filterNot { it.isLoopbackAddress }
                ?.map { it.address }
                ?.toList()
        }.getOrNull() ?: emptyList()

        if (localIps.isEmpty()) return null

        for (local in localIps) {
            val match24 = candidates.firstOrNull { c ->
                val b = c.address
                b.size == 4 && local.size == 4 && b[0] == local[0] && b[1] == local[1] && b[2] == local[2]
            }
            if (match24 != null) return match24
        }
        for (local in localIps) {
            val match16 = candidates.firstOrNull { c ->
                val b = c.address
                b.size == 4 && local.size == 4 && b[0] == local[0] && b[1] == local[1]
            }
            if (match16 != null) return match16
        }
        return null
    }

    private fun upsert(transform: (List<PcDevice>) -> List<PcDevice>) {
        _devices.value = transform(_devices.value)
    }
}


