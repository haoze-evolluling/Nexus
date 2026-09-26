package com.haoze.nexus.audio

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.haoze.nexus.R
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that hosts the low-latency UDP audio receiver,
 * managing session handshakes, Opus decoding, clock sync, and peer calibration.
 */
class AudioReceiverService : Service() {

    private companion object {
        const val TAG = "NexusReceiver"
        const val MAX_UDP_PACKET = 65535
        const val HEARTBEAT_TIMEOUT_NS = 3_500_000_000L
        fun timeSyncIntervalNs(hasEstimate: Boolean): Long = if (hasEstimate) 2_000_000_000L else 250_000_000L
    }

    @Volatile private var stopRequested = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var cachedSettings = AudioSettings()
    @Volatile private var cachedTrustedPcs = emptySet<String>()

    private var socket: DatagramSocket? = null
    private var worker: Thread? = null

    // Modular helper components
    private val notificationManager by lazy { AudioReceiverNotificationManager(this) }
    private val mediaSessionManager by lazy { AudioMediaSessionManager(this, TAG) }
    private val nsdRegistrar by lazy { AudioNsdRegistrar(this) }
    private val reconnectManager by lazy {
        AudioAutoReconnectManager(this, serviceScope, ::selfIdBlocking) { stopRequested }
    }
    private val sessionController by lazy { AudioSessionController(this, notificationManager) }
    private val peerCalibrationController = AudioPeerCalibrationController()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AudioReceiverNotificationManager.ACTION_RESPOND -> {
                val requestId = intent.getStringExtra(AudioReceiverNotificationManager.EXTRA_REQUEST_ID) ?: ""
                val allow = intent.getBooleanExtra(AudioReceiverNotificationManager.EXTRA_ALLOW, false)
                val remember = intent.getBooleanExtra(AudioReceiverNotificationManager.EXTRA_REMEMBER, false)
                if (requestId.isNotEmpty()) ConnectionBus.decisions.add(Triple(requestId, allow, remember))
            }
        }
        Log.i(TAG, "receiver service starting port=${NexusProtocol.port}")
        stopRequested = false
        serviceScope.launch {
            SettingsRepository(applicationContext).settings.collect { cachedSettings = it }
        }
        serviceScope.launch {
            PcTrustRepository(applicationContext).trusted.collect { cachedTrustedPcs = it.keys }
        }
        mediaSessionManager.ensureMediaSession()
        // Publish the media session-backed notification before doing network
        // work so Android treats this as an active lock-screen playback
        // service from the moment it is started.
        startForeground(
            AudioReceiverNotificationManager.FOREGROUND_NOTIFICATION_ID,
            notificationManager.createForegroundNotification(null)
        )
        val initialSettings = runBlocking { SettingsRepository(this@AudioReceiverService).settings.first() }
        nsdRegistrar.register(initialSettings)

        if (worker?.isAlive != true) {
            worker = thread(name = "nexus-udp") { receiveLoop() }
        }
        // Keep the receiver discoverable after the process is reclaimed while the
        // screen is locked; trusted peers can then reconnect without reopening UI.
        return START_STICKY
    }

    override fun onDestroy() {
        stopRequested = true
        reconnectManager.cancelAll()
        sessionController.activePc?.let { pc ->
            // 让电脑端立即断开会话，而不是等反馈超时。
            sessionController.sendBye(pc, socket, selfIdBlocking())
        }
        ConnectionBus.activePc.value = null
        mediaSessionManager.updatePlaybackState(false)
        ConnectionBus.authPrompt.value = null
        ConnectionBus.calibration.value = null
        ConnectionBus.peerCalibration.value = emptyMap()
        socket?.close()
        worker?.interrupt()
        nsdRegistrar.unregister()
        notificationManager.dismissAuthNotification()
        mediaSessionManager.release()
        sessionController.clear()
        peerCalibrationController.clearPeerOperation()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun selfIdBlocking(): String =
        runBlocking { SettingsRepository(applicationContext).settings.first().deviceId }

    private fun receiveLoop() {
        val repository = SettingsRepository(this@AudioReceiverService)
        var settings = runBlocking { repository.settings.first() }
        val track = AudioTrackFactory.createTrack()
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
        var highest = 0L
        var receivedCount = 0L
        var lostCount = 0L
        var lastFeedback = System.nanoTime()
        var fecPending = false
        var actualBitrate = settings.initialBitrateKbps * 1000
        val clockSync = ClockSyncEstimator()
        var lastTimeSyncSentNs = 0L
        var lastHeartbeatNs = System.nanoTime()
        var heartbeatPcId = ""
        var expectedNextTsNs = 0L
        var lastFrameMsNs = 10_000_000L
        val player = SynchronizedPlayer(track, trackFactory = { AudioTrackFactory.createTrack() })
        player.setClock { streamNs -> clockSync.mapToLocal(streamNs) }
        player.start()
        var playerOverflow = 0L

        fun offerToPlayer(pcm: ByteArray, tsNs: Long) {
            if (!player.offer(pcm, tsNs)) playerOverflow++
        }

        fun resetAudioSession() {
            sessionController.activePc = null
            activeSession = 0L
            highest = 0L
            receivedCount = 0L
            lostCount = 0L
            fecPending = false
            expectedNextTsNs = 0L
            ConnectionBus.activePc.value = null
            ConnectionBus.calibration.value = null
            mediaSessionManager.updatePlaybackState(false)
            notificationManager.updateForegroundNotification(null)
            buffer.clear()
            clockSync.reset()
            player.resetForSession()
            peerCalibrationController.clearPeerOperation()
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
            val pc = sessionController.activePc
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
                val currentPcId = sessionController.activePc?.deviceId ?: ""
                if (currentPcId != heartbeatPcId) {
                    heartbeatPcId = currentPcId
                    lastHeartbeatNs = System.nanoTime()
                }

                // 采纳连接器登记的发送方（手机主动连接电脑的场景）。
                ConnectionBus.queuedSender?.let { queued ->
                    ConnectionBus.queuedSender = null
                    sessionController.adoptPc(queued, socket, selfIdBlocking()) {
                        reconnectManager.cancelAutoReconnect(it)
                    }
                }

                // 处理用户授权决定。
                while (true) {
                    val decision = ConnectionBus.decisions.poll() ?: break
                    sessionController.applyDecision(
                        decision = decision,
                        socket = socket,
                        selfId = selfIdBlocking(),
                        onTrustDevice = { deviceId, name ->
                            serviceScope.launch {
                                runCatching { PcTrustRepository(applicationContext).trust(deviceId, name) }
                            }
                        },
                        onCancelReconnect = { reconnectManager.cancelAutoReconnect(it) }
                    )
                }

                peerCalibrationController.pollCalibrationRequests(
                    socket = socket,
                    activePc = sessionController.activePc,
                    selfId = selfIdBlocking()
                )
                peerCalibrationController.checkTimeouts(System.nanoTime())

                // 处理用户主动断开。
                while (true) {
                    val disconnectId = ConnectionBus.localDisconnects.poll() ?: break
                    reconnectManager.cancelAutoReconnect(disconnectId)
                    if (sessionController.activePc?.deviceId == disconnectId) {
                        val gone = sessionController.activePc
                        resetAudioSession()
                        gone?.let { sessionController.sendBye(it, socket, selfIdBlocking()) }
                    }
                    ConnectionBus.transition(disconnectId, ConnectionEvent.LOCAL_DISCONNECT)
                }

                sessionController.expirePrompts()

                // 周期性时钟同步：多设备对齐播放的基础。
                val syncTarget = sessionController.activePc
                if (syncTarget != null && syncTarget.port != 0 &&
                    System.nanoTime() - lastTimeSyncSentNs > timeSyncIntervalNs(clockSync.hasEstimate)
                ) {
                    runCatching {
                        val request = TimeSyncControl(TimeSyncControl.KIND_REQUEST, System.nanoTime(), 0, 0).encode()
                        socket?.send(DatagramPacket(request, request.size, syncTarget.address, syncTarget.port))
                    }
                    lastTimeSyncSentNs = System.nanoTime()
                }

                val datagram = DatagramPacket(bytes, bytes.size)
                var gotPacket = true
                try {
                    socket?.receive(datagram)
                } catch (_: java.net.SocketTimeoutException) {
                    gotPacket = false
                }

                if (!gotPacket) {
                    peerCalibrationController.checkTargetLocalReached(System.nanoTime())
                    if (peerCalibrationController.peerResetRequested) {
                        buffer.clear()
                        player.resetForSession()
                        expectedNextTsNs = 0L
                        peerCalibrationController.onResetBoundaryCompleted(
                            socket = socket,
                            activePc = sessionController.activePc,
                            selfId = selfIdBlocking()
                        )
                    }

                    // 无音频或心跳超过 3.5s：网络中断或电脑进入休眠，触发自动重连
                    if (sessionController.activePc != null && System.nanoTime() - lastHeartbeatNs > HEARTBEAT_TIMEOUT_NS) {
                        val gone = sessionController.activePc
                        val targetPc = gone?.let {
                            PcDevice(it.deviceId, it.name, it.address.hostAddress ?: "", NexusProtocol.desktopControlPort)
                        } ?: sessionController.lastConnectedPc

                        resetAudioSession()
                        // 异常中断切勿发送 sendBye，否则会主动杀死电脑端的推流会话
                        gone?.let {
                            ConnectionBus.transition(it.deviceId, ConnectionEvent.HEARTBEAT_TIMEOUT)
                            ConnectionBus.notify(R.string.msg_connection_interrupted, it.name)
                        }
                        if (targetPc != null) {
                            reconnectManager.startAutoReconnect(targetPc)
                        }
                    }

                    if (sessionController.activePc != null && System.nanoTime() - lastFeedback > 200_000_000L) {
                        AudioFeedbackSender.sendFeedback(
                            socket = socket,
                            address = lastAddress,
                            port = lastPort,
                            session = activeSession,
                            highest = highest,
                            received = receivedCount,
                            lost = lostCount,
                            queue = queueExcess(),
                            bitrate = actualBitrate,
                            syncState = currentSyncState(),
                            offsetMs = clockSync.relativeOffsetMs()?.toInt() ?: 0,
                            rttMs = clockSync.lastRttMs()?.toInt() ?: 0
                        )
                        lastFeedback = System.nanoTime()
                    }
                    publishCalibration()
                    continue
                }

                if (peerCalibrationController.peerResetRequested) {
                    // Keep the desktop session and its clock mapping intact; only
                    // discard locally queued audio at the peer-agreed boundary.
                    buffer.clear()
                    player.resetForSession()
                    expectedNextTsNs = 0L
                    peerCalibrationController.onResetBoundaryCompleted(
                        socket = socket,
                        activePc = sessionController.activePc,
                        selfId = selfIdBlocking()
                    )
                }

                val control = SettingsControl.decode(datagram.data, datagram.length)
                if (control != null && sessionController.fromActivePc(datagram.address, datagram.port)) {
                    val incoming = AudioSettings(control.bitrateKbps, control.frameMs, control.updatedAtMs, control.deviceId)
                    settings = incoming
                    serviceScope.launch { repository.applyIfNewer(incoming) }
                    continue
                }

                val heartbeat = HeartbeatControl.decode(datagram.data, datagram.length)
                if (heartbeat != null) {
                    val fromPc = sessionController.fromActivePc(datagram.address, datagram.port)
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
                if (conn != null) {
                    sessionController.handleConnControl(
                        conn = conn,
                        datagram = datagram,
                        socket = socket,
                        cachedTrustedPcs = cachedTrustedPcs,
                        selfId = selfIdBlocking(),
                        onResetAudioSession = ::resetAudioSession,
                        onCancelReconnect = { reconnectManager.cancelAutoReconnect(it) }
                    )
                    continue
                }

                val peerControl = PeerCalibrationControl.decode(datagram.data, datagram.length)
                if (peerControl != null) {
                    peerCalibrationController.handlePeerCalibration(
                        control = peerControl,
                        datagram = datagram,
                        socket = socket,
                        activePc = sessionController.activePc,
                        selfId = selfIdBlocking()
                    )
                    continue
                }

                val timeSync = TimeSyncControl.decode(datagram.data, datagram.length)
                if (timeSync != null) {
                    if (!sessionController.fromActivePc(datagram.address, datagram.port) &&
                        !peerCalibrationController.fromActivePeer(datagram.address)
                    ) continue

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
                val authorized = sessionController.fromActivePc(datagram.address, datagram.port)
                if (!authorized) {
                    unauthorizedDrops++
                    if (unauthorizedDrops % 100 == 1L) {
                        Log.w(TAG, "dropping audio from unauthorized ${datagram.address} (total=$unauthorizedDrops)")
                    }
                    continue
                }

                val packet = NexusProtocol.decode(datagram.data, datagram.length)
                if (packet == null) {
                    Log.w(TAG, "invalid UDP packet length=${datagram.length}")
                    continue
                }

                val currentActivePc = sessionController.activePc
                if (currentActivePc != null && (currentActivePc.port == 0 || lastAddress == null || activeSession != packet.session)) {
                    currentActivePc.address = datagram.address
                    currentActivePc.port = datagram.port
                }

                lastAddress = datagram.address
                lastPort = datagram.port
                lastHeartbeatNs = System.nanoTime()
                mediaSessionManager.updatePlaybackState(true)

                if (activeSession != 0L && activeSession != packet.session) {
                    Log.i(TAG, "new audio session $activeSession -> ${packet.session}; resetting playback timeline")
                    buffer.clear()
                    clockSync.reset()
                    player.resetForSession()
                    highest = 0
                    receivedCount = 0
                    lostCount = 0
                    fecPending = false
                    expectedNextTsNs = 0L
                }

                activeSession = packet.session
                actualBitrate = packet.bitrate
                if (packet.sequence > highest) {
                    lostCount += (packet.sequence - highest - if (receivedCount == 0L) 0 else 1).coerceAtLeast(0)
                    highest = packet.sequence
                }
                receivedCount++
                decoded++
                buffer.offer(packet)

                while (true) {
                    val item = buffer.takeItem() ?: break
                    when (item) {
                        is PacketJitterBuffer.Item.Packet -> {
                            val packetFrame = item.value
                            if (fecPending) {
                                val recovered = if ((packetFrame.flags and 1) != 0) {
                                    OpusNative.decode(decoderHandle, packetFrame.opus, true)
                                } else {
                                    OpusNative.decodePlc(decoderHandle, 480 * packetFrame.frameMilliseconds / 10)
                                }
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

                if (System.nanoTime() - lastFeedback > 200_000_000L) {
                    AudioFeedbackSender.sendFeedback(
                        socket = socket,
                        address = lastAddress,
                        port = lastPort,
                        session = activeSession,
                        highest = highest,
                        received = receivedCount,
                        lost = lostCount,
                        queue = queueExcess(),
                        bitrate = actualBitrate,
                        syncState = currentSyncState(),
                        offsetMs = clockSync.relativeOffsetMs()?.toInt() ?: 0,
                        rttMs = clockSync.lastRttMs()?.toInt() ?: 0
                    )
                    lastFeedback = System.nanoTime()
                }
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
}
