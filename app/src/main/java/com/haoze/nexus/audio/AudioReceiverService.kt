package com.haoze.nexus.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.haoze.nexus.R
import com.haoze.nexus.ui.audio.AudioReceiverActivity

class AudioReceiverService : Service() {
    private companion object {
        const val TAG = "NexusReceiver"; const val MAX_UDP_PACKET = 65535
        const val AUTH_NOTIFICATION_ID = 9
        const val PROMPT_EXPIRY_MS = 35_000L
        const val HEARTBEAT_TIMEOUT_NS = 3_500_000_000L
        const val ACTION_RESPOND = "com.haoze.nexus.audio.action.RESPOND"
        const val EXTRA_REQUEST_ID = "request_id"; const val EXTRA_ALLOW = "allow"; const val EXTRA_REMEMBER = "remember"
        fun timeSyncIntervalNs(hasEstimate: Boolean): Long = if (hasEstimate) 2_000_000_000L else 250_000_000L
    }

    @Volatile private var stopRequested = false
    private var socket: DatagramSocket? = null
    private var worker: Thread? = null
    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null
    private var activePc: ActivePc? = null
    private var mediaSession: MediaSession? = null
    @Volatile private var peerOperation = 0L
    @Volatile private var peerOperationStartedNs = 0L
    @Volatile private var peerTargetLocalNs = 0L
    @Volatile private var peerResetRequested = false
    @Volatile private var peerDeviceId = ""
    @Volatile private var peerOffsetMs: Long? = null
    @Volatile private var peerRttMs: Long? = null
    @Volatile private var lastPeerAddress: InetAddress? = null
    @Volatile private var lastPeerPort = 0
    @Volatile private var pendingPeerAddress: InetAddress? = null
    @Volatile private var pendingPeerPort = 0

    /** 仅在接收线程访问：等待用户决定的连接请求。 */
    private class PromptRecord(val prompt: PcAuthPrompt, val address: InetAddress, val port: Int, val nonce: Long)

    private val pendingPrompts = HashMap<String, PromptRecord>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESPOND -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
                val allow = intent.getBooleanExtra(EXTRA_ALLOW, false)
                val remember = intent.getBooleanExtra(EXTRA_REMEMBER, false)
                if (requestId.isNotEmpty()) ConnectionBus.decisions.add(Triple(requestId, allow, remember))
            }
        }
        Log.i(TAG, "receiver service starting port=${NexusProtocol.port}")
        stopRequested = false
        ensureMediaSession()
        // Publish the media session-backed notification before doing network
        // work so Android treats this as an active lock-screen playback
        // service from the moment it is started.
        startForeground(8, notification(null))
        registerService()
        if (worker?.isAlive != true) worker = thread(name = "nexus-udp") { receiveLoop() }
        // Keep the receiver discoverable after the process is reclaimed while the
        // screen is locked; trusted peers can then reconnect without reopening UI.
        return START_STICKY
    }

    override fun onDestroy() {
        stopRequested = true
        activePc?.let { pc ->
            // 让电脑端立即断开会话，而不是等反馈超时。
            sendBye(pc)
        }
        ConnectionBus.activePc.value = null
        updatePlaybackState(false)
        ConnectionBus.authPrompt.value = null
        ConnectionBus.calibration.value = null
        ConnectionBus.peerCalibration.value = emptyMap()
        socket?.close()
        worker?.interrupt()
        registration?.let { nsd?.unregisterService(it) }
        dismissAuthNotification()
        mediaSession?.release()
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private fun selfIdBlocking(): String = runBlocking { SettingsRepository(applicationContext).settings.first().deviceId }

    private fun notification(pcName: String?): Notification {
        val loc = LocaleManager.wrap(this)
        return NotificationCompat.Builder(this, "nexus-receiver")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(loc.getString(R.string.app_name))
            .setContentText(
                if (pcName == null) loc.getString(R.string.receiver_notification)
                else loc.getString(R.string.receiver_notification_active, pcName)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
            .also {
                val manager = getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel("nexus-receiver", loc.getString(R.string.receiver_channel), NotificationManager.IMPORTANCE_LOW)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                manager.createNotificationChannel(channel)
            }
    }

    private fun ensureMediaSession() {
        if (mediaSession != null) return
        mediaSession = MediaSession(this, TAG).also { session ->
            session.isActive = true
            session.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE).setState(PlaybackState.STATE_PAUSED, 0L, 0f).build())
        }
    }

    private fun updatePlaybackState(playing: Boolean) {
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        mediaSession?.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE).setState(state, 0L, if (playing) 1f else 0f).build())
    }

    private fun registerService() {
        // onStartCommand 在每次前台服务被拉起时都会触发（例如发起连接前的
        // ensureReceiverRunning）；重复注册同名服务会与自身记录冲突导致广播异常。
        if (registration != null) return
        val settings = runBlocking { SettingsRepository(this@AudioReceiverService).settings.first() }; nsd=getSystemService(Context.NSD_SERVICE) as NsdManager; val friendly=DeviceIdentity.friendlyName(LocaleManager.wrap(this)); val info=NsdServiceInfo().apply { serviceName="Nexus-$friendly"; serviceType="_nexus._udp."; port=NexusProtocol.port; setAttribute("role", "speaker"); setAttribute("device_id", settings.deviceId); setAttribute("codec", "opus"); setAttribute("sample_rate", NexusProtocol.sampleRate.toString()); setAttribute("channels", NexusProtocol.channels.toString()); setAttribute("bitrate", (settings.initialBitrateKbps * 1000).toString()); setAttribute("frame_ms", NexusProtocol.supportedFrameMilliseconds.joinToString(",")); setAttribute("current_frame_ms", settings.frameMs.toString()); setAttribute("settings_updated_at", settings.updatedAtMs.toString()); setAttribute("settings_device_id", settings.deviceId) }; registration=object:NsdManager.RegistrationListener { override fun onServiceRegistered(i:NsdServiceInfo){ Log.i(TAG,"advertising ${i.serviceName}") }; override fun onRegistrationFailed(i:NsdServiceInfo,e:Int){ Log.e(TAG,"NSD registration failed: $e"); registration = null }; override fun onServiceUnregistered(i:NsdServiceInfo){ registration = null }; override fun onUnregistrationFailed(i:NsdServiceInfo,e:Int){ Log.e(TAG,"NSD unregistration failed: $e") } }; nsd?.registerService(info,NsdManager.PROTOCOL_DNS_SD,registration) }

    private fun receiveLoop() {
        val repository = SettingsRepository(this@AudioReceiverService)
        val trust = PcTrustRepository(this@AudioReceiverService)
        var settings = runBlocking { repository.settings.first() }
        val track = newTrack()
        val buffer = PacketJitterBuffer(targetPackets = 2)
        socket = DatagramSocket(NexusProtocol.port)
        val bytes = ByteArray(MAX_UDP_PACKET)
        var received = 0L
        var decoded = 0L
        var unauthorizedDrops = 0L
        val decoderHandle = OpusNative.createDecoder(48000, 2)
        check(decoderHandle != 0L) { "native Opus decoder unavailable" }
        var lastAddress: InetAddress? = null
        var lastPort = 0
        var activeSession = 0L
        var highest = 0L; var receivedCount = 0L; var lostCount = 0L; var lastFeedback = System.nanoTime()
        var fecPending = false
        var actualBitrate = settings.initialBitrateKbps * 1000
        val clockSync = ClockSyncEstimator()
        var lastTimeSyncSentNs = 0L
        var lastAudioNs = System.nanoTime()
        var lastHeartbeatNs = System.nanoTime()
        var heartbeatPcId = ""
        var expectedNextTsNs = 0L
        var lastFrameMsNs = 10_000_000L
        val player = SynchronizedPlayer(track, trackFactory = { newTrack() })
        player.setClock { streamNs -> clockSync.mapToLocal(streamNs) }
        player.start()
        var playerOverflow = 0L
        fun offerToPlayer(pcm: ByteArray, tsNs: Long) {
            if (!player.offer(pcm, tsNs)) playerOverflow++
        }
        fun fromActivePc(address: InetAddress, port: Int): Boolean {
            val pc = activePc ?: return false
            // The sender's ephemeral port is learned from its authenticated
            // SVCR request. Never accept control traffic from the mDNS address
            // alone, and never use port 0 as a wildcard.
            return pc.port != 0 && pc.address == address && pc.port == port
        }
        fun fromActivePeer(address: InetAddress, port: Int): Boolean {
            // Peer calibration probes use the other receiver's ephemeral
            // socket, so their source port is deliberately not the peer
            // receiver's fixed service port. The prior SVAC handshake has
            // already authenticated the peer address for this operation.
            return lastPeerAddress == address
        }
        fun publishPeer(deviceId: String, state: PeerCalibrationState) {
            ConnectionBus.peerCalibration.value = ConnectionBus.peerCalibration.value + (deviceId to state)
        }
        fun clearPeerOperation() {
            peerOperation = 0L; peerOperationStartedNs = 0L; peerTargetLocalNs = 0L; peerResetRequested = false
            peerDeviceId = ""; peerOffsetMs = null; peerRttMs = null; pendingPeerAddress = null; pendingPeerPort = 0
        }
        // 反馈的队列值表示超出同步预算的多余积压，而非总缓冲：
        // 桌面端以 queue>1 为拥塞信号，直接上报总缓冲会被误判持续降码率。
        fun queueExcess(): Int {
            val expectedBacklog = (player.latencyBudgetNs / lastFrameMsNs).toInt()
            return (buffer.queuedPackets() + player.backlogFrames() - expectedBacklog).coerceAtLeast(0)
        }
        // 校准阶段：音频已到但时钟未收敛=计算中；收敛后等待/开始写入=同步；
        // 已写入 AudioTrack=完成。桌面端据此驱动多设备同步校准动画。
        fun currentSyncState(): Int = when {
            player.playedFrames > 0L -> SyncState.PLAYING
            clockSync.hasStableEstimate -> SyncState.ALIGNED
            received > 0L -> SyncState.CALIBRATING
            else -> SyncState.UNKNOWN
        }
        fun publishCalibration() {
            val pc = activePc
            if (pc == null) {
                if (ConnectionBus.calibration.value != null) ConnectionBus.calibration.value = null
                return
            }
            val state = currentSyncState()
            val phase = when (state) {
                SyncState.PLAYING -> CalibrationPhase.DONE
                SyncState.ALIGNED -> CalibrationPhase.SYNC
                SyncState.CALIBRATING -> CalibrationPhase.CALCULATE
                else -> CalibrationPhase.DETECT
            }
            val withStats = phase == CalibrationPhase.SYNC || phase == CalibrationPhase.DONE
            val next = CalibrationState(
                pcName = pc.name,
                phase = phase,
                offsetMs = if (withStats) clockSync.relativeOffsetMs() else null,
                rttMs = if (withStats) clockSync.lastRttMs() else null,
            )
            if (ConnectionBus.calibration.value != next) ConnectionBus.calibration.value = next
        }
        try {
            socket?.soTimeout = 50
            while (!stopRequested && !Thread.currentThread().isInterrupted) {
                val currentPcId = activePc?.deviceId ?: ""
                if (currentPcId != heartbeatPcId) {
                    heartbeatPcId = currentPcId
                    lastHeartbeatNs = System.nanoTime()
                }
                // 采纳连接器登记的发送方（手机主动连接电脑的场景）。
                ConnectionBus.queuedSender?.let { queued ->
                    ConnectionBus.queuedSender = null
                    if (activePc?.deviceId != queued.deviceId) adoptPc(queued)
                }
                // 处理用户授权决定。
                while (true) {
                    val decision = ConnectionBus.decisions.poll() ?: break
                    applyDecision(decision)
                }
                while (true) {
                    val peer = ConnectionBus.peerCalibrationRequests.poll() ?: break
                    val pc = activePc
                    if (pc == null || peerOperation != 0L) {
                        publishPeer(peer.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                        continue
                    }
                    peerOperation = java.security.SecureRandom().nextLong().let { if (it == 0L) 1L else it }
                    peerOperationStartedNs = System.nanoTime()
                    peerDeviceId = peer.deviceId
                    pendingPeerAddress = InetAddress.getByName(peer.host); pendingPeerPort = peer.port
                    publishPeer(peer.deviceId, PeerCalibrationState(PeerCalibrationPhase.REQUESTING))
                    val request = PeerCalibrationControl(PeerCalibrationControl.REQUEST, peerOperation, selfIdBlocking(), pc.deviceId).encode()
                    runCatching { socket?.send(DatagramPacket(request, request.size, InetAddress.getByName(peer.host), peer.port)) }
                        .onFailure { publishPeer(peer.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED)); clearPeerOperation() }
                }
                if (peerOperation != 0L && peerTargetLocalNs != 0L && System.nanoTime() >= peerTargetLocalNs) peerResetRequested = true
                if (peerOperation != 0L && peerOperationStartedNs != 0L && System.nanoTime() - peerOperationStartedNs > 12_000_000_000L) {
                    publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED)); clearPeerOperation()
                }
                // 处理用户主动断开。
                while (true) {
                    val disconnectId = ConnectionBus.localDisconnects.poll() ?: break
                    if (activePc?.deviceId == disconnectId) {
                        val gone = activePc
                        activePc = null
                        ConnectionBus.activePc.value = null
                        updatePlaybackState(false)
                        updateForegroundNotification(null)
                        gone?.let(::sendBye)
                        ConnectionBus.transition(disconnectId, ConnectionEvent.LOCAL_DISCONNECT)
                    }
                }
                expirePrompts()
                // 周期性时钟同步：多设备对齐播放的基础。
                val syncTarget = activePc
                if (syncTarget != null && syncTarget.port != 0 && System.nanoTime() - lastTimeSyncSentNs > timeSyncIntervalNs(clockSync.hasEstimate)) {
                    runCatching {
                        val request = TimeSyncControl(TimeSyncControl.KIND_REQUEST, System.nanoTime(), 0, 0).encode()
                        socket?.send(DatagramPacket(request, request.size, syncTarget.address, syncTarget.port))
                    }
                    lastTimeSyncSentNs = System.nanoTime()
                }
                val datagram = DatagramPacket(bytes, bytes.size)
                var gotPacket = true
                try { socket?.receive(datagram) } catch (_: java.net.SocketTimeoutException) { gotPacket = false }
                if (!gotPacket) {
                    if (peerOperation != 0L && peerTargetLocalNs != 0L && System.nanoTime() >= peerTargetLocalNs) peerResetRequested = true
                    if (peerResetRequested) {
                        buffer.clear(); player.resetForSession(); expectedNextTsNs = 0L
                        peerResetRequested = false
                        // peerOffsetMs is a boot-relative clock-origin conversion value;
                        // keep it internal and never expose it as a playback deviation.
                        publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.COMPLETE, null, peerRttMs))
                        val pc = activePc
                        val address = lastPeerAddress
                        if (pc != null && address != null && lastPeerPort != 0) {
                            val done = PeerCalibrationControl(PeerCalibrationControl.COMPLETE, peerOperation, selfIdBlocking(), pc.deviceId, peerTargetLocalNs, (peerOffsetMs ?: 0L) * 1_000_000L, peerRttMs ?: 0L).encode()
                            runCatching { socket?.send(DatagramPacket(done, done.size, address, lastPeerPort)) }
                        }
                        clearPeerOperation()
                    }
                    // 无音频超过 10s：电脑端可能异常退出或网络中断，主动清理连接状态。
                    if (activePc != null && System.nanoTime() - lastHeartbeatNs > HEARTBEAT_TIMEOUT_NS) {
                        val gone = activePc
                        activePc = null
                        ConnectionBus.activePc.value = null
                        updatePlaybackState(false)
                        updateForegroundNotification(null)
                        // 通知电脑端立即 teardown，避免两端连接状态不一致。
                        gone?.let(::sendBye)
                        gone?.let { ConnectionBus.transition(it.deviceId, ConnectionEvent.HEARTBEAT_TIMEOUT) }
                        ConnectionBus.notify(R.string.msg_connection_interrupted, gone?.name ?: LocaleManager.wrap(this).getString(R.string.generic_pc))
                    }
                    if (activePc != null && System.nanoTime() - lastFeedback > 200_000_000L) { sendFeedback(lastAddress, lastPort, activeSession, highest, receivedCount, lostCount, queueExcess(), actualBitrate, currentSyncState(), clockSync.relativeOffsetMs()?.toInt() ?: 0, clockSync.lastRttMs()?.toInt() ?: 0); lastFeedback = System.nanoTime() }
                    publishCalibration()
                    continue
                }
                if (peerResetRequested) {
                    // Keep the desktop session and its clock mapping intact; only
                    // discard locally queued audio at the peer-agreed boundary.
                    buffer.clear(); player.resetForSession(); expectedNextTsNs = 0L
                    peerResetRequested = false
                    publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.COMPLETE, null, peerRttMs))
                    val pc = activePc
                    if (pc != null) {
                        val done = PeerCalibrationControl(PeerCalibrationControl.COMPLETE, peerOperation, selfIdBlocking(), pc.deviceId, peerTargetLocalNs, (peerOffsetMs ?: 0L) * 1_000_000L, peerRttMs ?: 0L).encode()
                        runCatching { socket?.send(DatagramPacket(done, done.size, lastPeerAddress ?: return@runCatching, lastPeerPort)) }
                    }
                    clearPeerOperation()
                }
                val control = SettingsControl.decode(datagram.data, datagram.length)
                if (control != null && fromActivePc(datagram.address, datagram.port)) {
                    val incoming = AudioSettings(control.bitrateKbps, control.frameMs, control.updatedAtMs, control.deviceId)
                    if (runBlocking { repository.applyIfNewer(incoming) }) settings = incoming
                    continue
                }
                val heartbeat = HeartbeatControl.decode(datagram.data, datagram.length)
                if (heartbeat != null) {
                    val fromPc = fromActivePc(datagram.address, datagram.port)
                    if (fromPc && (activeSession == 0L || heartbeat.session == activeSession)) {
                        if (activeSession == 0L) activeSession = heartbeat.session
                        lastHeartbeatNs = System.nanoTime()
                    }
                    if (fromPc && heartbeat.kind == HeartbeatControl.KIND_PING) {
                        val pong = HeartbeatControl(HeartbeatControl.KIND_PONG, heartbeat.session, heartbeat.sequence, System.nanoTime()).encode()
                        runCatching { socket?.send(DatagramPacket(pong, pong.size, datagram.address, datagram.port)) }
                    }
                    continue
                }
                val conn = ConnControl.decode(datagram.data, datagram.length)
                if (conn != null) { handleConnControl(conn, datagram, trust); continue }
                val peerControl = PeerCalibrationControl.decode(datagram.data, datagram.length)
                if (peerControl != null) {
                    handlePeerCalibration(peerControl, datagram, ::publishPeer, ::clearPeerOperation)
                    continue
                }
                val timeSync = TimeSyncControl.decode(datagram.data, datagram.length)
                if (timeSync != null) {
                    if (!fromActivePc(datagram.address, datagram.port) && !fromActivePeer(datagram.address, datagram.port)) continue
                    if (timeSync.kind == TimeSyncControl.KIND_REQUEST) {
                        val t2 = System.nanoTime()
                        val response = TimeSyncControl(TimeSyncControl.KIND_RESPONSE, timeSync.t1, t2, System.nanoTime()).encode()
                        runCatching { socket?.send(DatagramPacket(response, response.size, datagram.address, datagram.port)) }
                    } else {
                        clockSync.onExchange(timeSync.t1, timeSync.t2, timeSync.t3, System.nanoTime())
                    }
                    continue
                }
                received++
                // 音频门控：只播放已授权发送方的数据。
                // For a phone-initiated session the peer was discovered via
                // mDNS, but the desktop may send from another interface. The
                // control handshake already authorized this peer; bind the
                // actual source address on its first audio packet.
                val authorized = fromActivePc(datagram.address, datagram.port)
                if (!authorized) { unauthorizedDrops++; if (unauthorizedDrops % 100 == 1L) Log.w(TAG, "dropping audio from unauthorized ${datagram.address} (total=$unauthorizedDrops)"); continue }
                val packet = NexusProtocol.decode(datagram.data, datagram.length)
                if (packet == null) { Log.w(TAG, "invalid UDP packet length=${datagram.length}"); continue }
                // The control request originates from the receiver's fixed
                // port (40125), while the desktop streams from the sender's
                // own ephemeral UDP socket. Bind feedback/time-sync to that
                // actual source on the first authenticated audio packet.
                // Keeping the control port here drops all feedback on the
                // floor and makes the desktop mark the session as interrupted.
                if (lastAddress == null || activeSession != packet.session) {
                    activePc!!.address = datagram.address
                    activePc!!.port = datagram.port
                }
                lastAddress = datagram.address; lastPort = datagram.port
                lastAudioNs = System.nanoTime()
                updatePlaybackState(true)
                if (activeSession != 0L && activeSession != packet.session) {
                    Log.i(TAG, "new audio session $activeSession -> ${packet.session}; resetting playback timeline")
                    buffer.clear()
                    clockSync.reset()
                    player.resetForSession()
                    highest = 0; receivedCount = 0; lostCount = 0; fecPending = false; expectedNextTsNs = 0L
                }
                activeSession = packet.session
                actualBitrate = packet.bitrate
                if (packet.sequence > highest) { lostCount += (packet.sequence - highest - if (receivedCount == 0L) 0 else 1).coerceAtLeast(0); highest = packet.sequence }
                receivedCount++
                decoded++
                buffer.offer(packet)
                while (true) {
                    val item = buffer.takeItem() ?: break
                    when (item) {
                        is PacketJitterBuffer.Item.Packet -> {
                            val packetFrame = item.value
                            if (fecPending) {
                                val recovered = if ((packetFrame.flags and 1) != 0) OpusNative.decode(decoderHandle, packetFrame.opus, true) else OpusNative.decodePlc(decoderHandle, 480 * packetFrame.frameMilliseconds / 10)
                                fecPending = false
                                if (recovered != null) offerToPlayer(recovered, expectedNextTsNs)
                            }
                            val pcm = OpusNative.decode(decoderHandle, packetFrame.opus, false)
                            if (pcm != null) offerToPlayer(pcm, packetFrame.timestampNs)
                            expectedNextTsNs = packetFrame.timestampNs + packetFrame.frameMilliseconds * 1_000_000L
                            lastFrameMsNs = packetFrame.frameMilliseconds * 1_000_000L
                        }
                        PacketJitterBuffer.Item.Gap -> {
                            lostCount++
                            fecPending = true
                            expectedNextTsNs += lastFrameMsNs
                        }
                    }
                }
                if (System.nanoTime() - lastFeedback > 200_000_000L) { sendFeedback(lastAddress, lastPort, activeSession, highest, receivedCount, lostCount, queueExcess(), actualBitrate, currentSyncState(), clockSync.relativeOffsetMs()?.toInt() ?: 0, clockSync.lastRttMs()?.toInt() ?: 0); lastFeedback = System.nanoTime() }
                publishCalibration()
            }
        } catch (e: Exception) {
            if (!stopRequested) Log.e(TAG, "receiver loop stopped", e)
        } finally {
            player.close()
            OpusNative.destroyDecoder(decoderHandle)
            Log.i(TAG, "receiver stopped received=$received decoded=$decoded unauthorized=$unauthorizedDrops overflow=$playerOverflow droppedLate=${player.droppedLateFrames} played=${player.playedFrames}")
            track.stop()
            track.release()
        }
    }

    /** 接收线程：处理连接控制报文。 */
    private fun handleConnControl(conn: ConnControl, datagram: DatagramPacket, trust: PcTrustRepository) {
        when (conn.kind) {
            ConnControl.KIND_REQUEST -> {
                val current = activePc
                if (current != null && current.deviceId != conn.deviceId) {
                    // A second PC cannot replace a live session implicitly.
                    respondConn(datagram.address, datagram.port, allow = false, nonce = conn.nonce)
                    ConnectionBus.transition(conn.deviceId, ConnectionEvent.DENIED)
                    return
                }
                val duplicate = pendingPrompts.values.firstOrNull {
                    it.prompt.deviceId == conn.deviceId && it.nonce == conn.nonce &&
                        it.address == datagram.address && it.port == datagram.port
                }
                if (duplicate != null) return
                val name = conn.name.ifBlank { conn.deviceId.take(8) }
                val trusted = runBlocking { trust.isTrusted(conn.deviceId) }
                if (activePc?.deviceId == conn.deviceId || trusted) {
                    respondConn(datagram.address, datagram.port, allow = true, nonce = conn.nonce)
                    adoptPc(ActivePc(conn.deviceId, name, datagram.address, datagram.port, conn.nonce))
                } else {
                    val requestId = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
                    val prompt = PcAuthPrompt(requestId, conn.deviceId, name, datagram.address.hostAddress ?: "", System.currentTimeMillis())
                    pendingPrompts[requestId] = PromptRecord(prompt, datagram.address, datagram.port, conn.nonce)
                    ConnectionBus.authPrompt.value = prompt
                    postAuthNotification(prompt)
                }
            }
            ConnControl.KIND_BYE -> {
                if (activePc?.deviceId == conn.deviceId && activePc?.nonce == conn.nonce) {
                    val gone = activePc
                    activePc = null
                    ConnectionBus.activePc.value = null
                    updatePlaybackState(false)
                    updateForegroundNotification(null)
                    peerOperation = 0L
                    peerTargetLocalNs = 0L
                    peerResetRequested = false
                    ConnectionBus.peerCalibration.value = emptyMap()
                    ConnectionBus.notify(R.string.msg_disconnected, gone?.name ?: LocaleManager.wrap(this).getString(R.string.generic_pc))
                    ConnectionBus.transition(conn.deviceId, ConnectionEvent.REMOTE_BYE)
                }
            }
            ConnControl.KIND_RESPONSE -> {} // 响应发给发起连接的临时 socket，不会到这里
        }
    }

    private fun applyDecision(decision: Triple<String, Boolean, Boolean>) {
        val record = pendingPrompts.remove(decision.first) ?: return
        ConnectionBus.authPrompt.value = null
        dismissAuthNotification()
        respondConn(record.address, record.port, decision.second, record.nonce)
        if (decision.second) {
            if (decision.third) runBlocking { PcTrustRepository(applicationContext).trust(record.prompt.deviceId, record.prompt.name) }
            adoptPc(ActivePc(record.prompt.deviceId, record.prompt.name, record.address, record.port, record.nonce))
        }
    }

    private fun expirePrompts() {
        if (pendingPrompts.isEmpty()) return
        val now = System.currentTimeMillis()
        val expired = pendingPrompts.values.filter { now - it.prompt.createdAtMs > PROMPT_EXPIRY_MS }
        for (record in expired) pendingPrompts.remove(record.prompt.requestId)
        if (expired.isNotEmpty()) {
            // 只清理确实过期的请求；重传产生的新请求弹窗不能被旧记录的过期连带关闭。
            val current = ConnectionBus.authPrompt.value
            if (current == null || expired.any { it.prompt.requestId == current.requestId }) {
                ConnectionBus.authPrompt.value = null
                dismissAuthNotification()
            }
        }
    }

    private fun adoptPc(pc: ActivePc) {
        activePc = pc
        ConnectionBus.activePc.value = pc
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.REQUEST_RECEIVED)
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.AUTHORIZED)
        updateForegroundNotification(pc.name)
        ConnectionBus.notify(R.string.msg_connected, pc.name)
    }

    private fun handlePeerCalibration(
        control: PeerCalibrationControl,
        datagram: DatagramPacket,
        publish: (String, PeerCalibrationState) -> Unit,
        clear: () -> Unit,
    ) {
        val pc = activePc ?: return
        when (control.kind) {
            PeerCalibrationControl.REQUEST -> {
                if (!acceptsPeerCalibrationRequest(control.pcId, pc.deviceId, peerOperation)) {
                    val reject = PeerCalibrationControl(PeerCalibrationControl.REJECT, control.operation, selfIdBlocking(), control.pcId).encode()
                    runCatching { socket?.send(DatagramPacket(reject, reject.size, datagram.address, datagram.port)) }
                    return
                }
                peerOperation = control.operation
                peerOperationStartedNs = System.nanoTime()
                peerDeviceId = control.deviceId
                lastPeerAddress = datagram.address
                lastPeerPort = datagram.port
                publish(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.MEASURING))
                val accept = PeerCalibrationControl(PeerCalibrationControl.ACCEPT, control.operation, selfIdBlocking(), control.pcId).encode()
                runCatching { socket?.send(DatagramPacket(accept, accept.size, datagram.address, datagram.port)) }
                    .onFailure { publish(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED)); clear() }
            }
            PeerCalibrationControl.ACCEPT -> {
                if (control.operation != peerOperation || control.deviceId != peerDeviceId || control.pcId != pc.deviceId || datagram.address != pendingPeerAddress || datagram.port != pendingPeerPort) return
                publish(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.MEASURING))
                lastPeerAddress = datagram.address; lastPeerPort = datagram.port
                val peer = AndroidDevice(control.deviceId, control.deviceId.take(8), datagram.address.hostAddress ?: return, datagram.port)
                thread(name = "nexus-peer-calibration") {
                    val result = runCatching { AndroidClockSync.query(peer) }.getOrNull()
                    val currentPc = activePc
                    if (result == null || currentPc == null || currentPc.deviceId != control.pcId || peerOperation != control.operation) {
                        publish(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED)); clear(); return@thread
                    }
                    val target = System.nanoTime() + 1_000_000_000L
                    peerTargetLocalNs = target; peerOffsetMs = result.offsetMs; peerRttMs = result.rttMs
                    val remoteTarget = target + result.offsetMs * 1_000_000L
                    val commit = PeerCalibrationControl(PeerCalibrationControl.COMMIT, control.operation, selfIdBlocking(), currentPc.deviceId, remoteTarget, result.offsetMs * 1_000_000L, result.rttMs).encode()
                    runCatching { socket?.send(DatagramPacket(commit, commit.size, datagram.address, datagram.port)) }
                        .onFailure { publish(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED)); clear() }
                    publish(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.WAITING_TARGET, result.offsetMs, result.rttMs))
                }
            }
            PeerCalibrationControl.REJECT, PeerCalibrationControl.CANCEL -> if (control.operation == peerOperation) {
                publish(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED)); clear()
            }
            PeerCalibrationControl.COMMIT -> {
                if (control.pcId != pc.deviceId || control.operation != peerOperation || control.deviceId != peerDeviceId || datagram.address != lastPeerAddress || datagram.port != lastPeerPort) return
                peerTargetLocalNs = control.targetNs; peerOffsetMs = -control.offsetNs / 1_000_000L; peerRttMs = control.rttMs
                publish(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.WAITING_TARGET, peerOffsetMs, peerRttMs))
            }
            PeerCalibrationControl.COMPLETE -> if (control.operation == peerOperation && control.deviceId == peerDeviceId) {
                publish(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.COMPLETE, peerOffsetMs, peerRttMs))
            }
        }
    }

    /** The service owns the active session, so it is the only safe place to attach its nonce. */
    private fun sendBye(pc: ActivePc) {
        runCatching {
            val bye = ConnControl(ConnControl.KIND_BYE, selfIdBlocking(), nonce = pc.nonce).encode()
            repeat(3) {
                socket?.send(DatagramPacket(bye, bye.size, pc.address, NexusProtocol.desktopControlPort))
            }
        }
    }

    private fun respondConn(address: InetAddress, port: Int, allow: Boolean, nonce: Long) {
        try {
            val selfId = runBlocking { SettingsRepository(applicationContext).settings.first().deviceId }
            val response = ConnControl(ConnControl.KIND_RESPONSE, selfId, allow = allow, nonce = nonce).encode()
            socket?.send(DatagramPacket(response, response.size, address, port))
        } catch (e: Exception) {
            Log.w(TAG, "respond conn failed: ${e.message}")
        }
    }

    private fun respondIntent(prompt: PcAuthPrompt, allow: Boolean, remember: Boolean): PendingIntent {
        val intent = Intent(this, AudioReceiverService::class.java).setAction(ACTION_RESPOND)
            .putExtra(EXTRA_REQUEST_ID, prompt.requestId)
            .putExtra(EXTRA_ALLOW, allow)
            .putExtra(EXTRA_REMEMBER, remember)
        return PendingIntent.getService(this, (prompt.requestId + allow + remember).hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun postAuthNotification(prompt: PcAuthPrompt) {
        val manager = getSystemService(NotificationManager::class.java)
        val loc = LocaleManager.wrap(this)
        val authChannel = NotificationChannel("nexus-auth", loc.getString(R.string.auth_channel), NotificationManager.IMPORTANCE_HIGH)
        authChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(authChannel)
        val openApp = PendingIntent.getActivity(
            this,
            prompt.requestId.hashCode(),
            Intent(this, AudioReceiverActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, "nexus-auth")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(loc.getString(R.string.auth_title))
            .setContentText(loc.getString(R.string.auth_notification_text, prompt.name))
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .addAction(0, loc.getString(R.string.auth_deny), respondIntent(prompt, false, false))
            .addAction(0, loc.getString(R.string.auth_allow), respondIntent(prompt, true, false))
            .addAction(0, loc.getString(R.string.auth_always_allow), respondIntent(prompt, true, true))
            .build()
        manager.notify(AUTH_NOTIFICATION_ID, notification)
    }

    private fun dismissAuthNotification() {
        getSystemService(NotificationManager::class.java).cancel(AUTH_NOTIFICATION_ID)
    }

    private fun updateForegroundNotification(pcName: String?) {
        runCatching { getSystemService(NotificationManager::class.java).notify(8, notification(pcName)) }
    }

    private fun sendFeedback(address: InetAddress?, port: Int, session: Long, highest: Long, received: Long, lost: Long, queue: Int, bitrate: Int, syncState: Int = SyncState.UNKNOWN, offsetMs: Int = 0, rttMs: Int = 0) { if (address == null || port == 0) return; try { val b=ReceiverFeedback(session, highest, received, lost, queue, bitrate, syncState, offsetMs, rttMs).encode(); socket?.send(DatagramPacket(b,b.size,address,port)) } catch (_: Exception) {} }
    private fun newTrack(): AudioTrack { val min=AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT); check(min > 0) { "AudioTrack buffer size unavailable: $min" }; return AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(min * 2).setTransferMode(AudioTrack.MODE_STREAM).build().also { it.play(); Log.i(TAG,"AudioTrack started buffer=$min configured=${min * 2} state=${it.state}") } }
}

