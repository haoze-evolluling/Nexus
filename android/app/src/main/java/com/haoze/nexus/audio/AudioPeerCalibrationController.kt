package com.haoze.nexus.audio

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import kotlin.concurrent.thread

/**
 * Coordinates peer-to-peer calibration protocol between multiple Android speaker receivers.
 */
class AudioPeerCalibrationController {

    @Volatile var peerOperation = 0L; private set
    @Volatile var peerOperationStartedNs = 0L; private set
    @Volatile var peerTargetLocalNs = 0L; private set
    @Volatile var peerResetRequested = false; private set
    @Volatile var peerDeviceId = ""; private set
    @Volatile var peerOffsetMs: Long? = null; private set
    @Volatile var peerRttMs: Long? = null; private set
    @Volatile var lastPeerAddress: InetAddress? = null; private set
    @Volatile var lastPeerPort = 0; private set
    @Volatile var pendingPeerAddress: InetAddress? = null; private set
    @Volatile var pendingPeerPort = 0; private set

    private val random = SecureRandom()

    fun fromActivePeer(address: InetAddress): Boolean {
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
        peerOperation = 0L
        peerOperationStartedNs = 0L
        peerTargetLocalNs = 0L
        peerResetRequested = false
        peerDeviceId = ""
        peerOffsetMs = null
        peerRttMs = null
        pendingPeerAddress = null
        pendingPeerPort = 0
    }

    fun pollCalibrationRequests(
        socket: DatagramSocket?,
        activePc: ActivePc?,
        selfId: String
    ) {
        while (true) {
            val peer = ConnectionBus.peerCalibrationRequests.poll() ?: break
            if (activePc == null || peerOperation != 0L) {
                publishPeer(peer.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                continue
            }
            peerOperation = random.nextLong().let { if (it == 0L) 1L else it }
            peerOperationStartedNs = System.nanoTime()
            peerDeviceId = peer.deviceId
            pendingPeerAddress = InetAddress.getByName(peer.host)
            pendingPeerPort = peer.port
            publishPeer(peer.deviceId, PeerCalibrationState(PeerCalibrationPhase.REQUESTING))
            val request = PeerCalibrationControl(
                PeerCalibrationControl.REQUEST,
                peerOperation,
                selfId,
                activePc.deviceId
            ).encode()
            runCatching {
                socket?.send(DatagramPacket(request, request.size, InetAddress.getByName(peer.host), peer.port))
            }.onFailure {
                publishPeer(peer.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                clearPeerOperation()
            }
        }
    }

    fun checkTimeouts(nowNs: Long) {
        if (peerOperation != 0L && peerTargetLocalNs != 0L && nowNs >= peerTargetLocalNs) {
            peerResetRequested = true
        }
        if (peerOperation != 0L && peerOperationStartedNs != 0L && nowNs - peerOperationStartedNs > 12_000_000_000L) {
            publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
            clearPeerOperation()
        }
    }

    fun checkTargetLocalReached(nowNs: Long) {
        if (peerOperation != 0L && peerTargetLocalNs != 0L && nowNs >= peerTargetLocalNs) {
            peerResetRequested = true
        }
    }

    fun onResetBoundaryCompleted(
        socket: DatagramSocket?,
        activePc: ActivePc?,
        selfId: String
    ) {
        peerResetRequested = false
        // peerOffsetMs is a boot-relative clock-origin conversion value;
        // keep it internal and never expose it as a playback deviation.
        publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.COMPLETE, null, peerRttMs))
        val address = lastPeerAddress
        if (activePc != null && address != null && lastPeerPort != 0) {
            val done = PeerCalibrationControl(
                PeerCalibrationControl.COMPLETE,
                peerOperation,
                selfId,
                activePc.deviceId,
                peerTargetLocalNs,
                (peerOffsetMs ?: 0L) * 1_000_000L,
                peerRttMs ?: 0L
            ).encode()
            runCatching { socket?.send(DatagramPacket(done, done.size, address, lastPeerPort)) }
        }
        clearPeerOperation()
    }

    fun handlePeerCalibration(
        control: PeerCalibrationControl,
        datagram: DatagramPacket,
        socket: DatagramSocket?,
        activePc: ActivePc?,
        selfId: String
    ) {
        val pc = activePc ?: return
        when (control.kind) {
            PeerCalibrationControl.REQUEST -> {
                if (!acceptsPeerCalibrationRequest(control.pcId, pc.deviceId, peerOperation)) {
                    val reject = PeerCalibrationControl(
                        PeerCalibrationControl.REJECT,
                        control.operation,
                        selfId,
                        control.pcId
                    ).encode()
                    runCatching { socket?.send(DatagramPacket(reject, reject.size, datagram.address, datagram.port)) }
                    return
                }
                peerOperation = control.operation
                peerOperationStartedNs = System.nanoTime()
                peerDeviceId = control.deviceId
                lastPeerAddress = datagram.address
                lastPeerPort = datagram.port
                publishPeer(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.MEASURING))
                val accept = PeerCalibrationControl(
                    PeerCalibrationControl.ACCEPT,
                    control.operation,
                    selfId,
                    control.pcId
                ).encode()
                runCatching { socket?.send(DatagramPacket(accept, accept.size, datagram.address, datagram.port)) }
                    .onFailure {
                        publishPeer(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                        clearPeerOperation()
                    }
            }
            PeerCalibrationControl.ACCEPT -> {
                if (control.operation != peerOperation || control.deviceId != peerDeviceId ||
                    control.pcId != pc.deviceId || datagram.address != pendingPeerAddress ||
                    datagram.port != pendingPeerPort
                ) return

                publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.MEASURING))
                lastPeerAddress = datagram.address
                lastPeerPort = datagram.port
                val peer = AndroidDevice(
                    control.deviceId,
                    control.deviceId.take(8),
                    datagram.address.hostAddress ?: return,
                    datagram.port
                )
                thread(name = "nexus-peer-calibration") {
                    val result = runCatching { AndroidClockSync.query(peer) }.getOrNull()
                    if (result == null || pc.deviceId != control.pcId || peerOperation != control.operation) {
                        publishPeer(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                        clearPeerOperation()
                        return@thread
                    }
                    val target = System.nanoTime() + 1_000_000_000L
                    peerTargetLocalNs = target
                    peerOffsetMs = result.offsetMs
                    peerRttMs = result.rttMs
                    val remoteTarget = target + result.offsetMs * 1_000_000L
                    val commit = PeerCalibrationControl(
                        PeerCalibrationControl.COMMIT,
                        control.operation,
                        selfId,
                        pc.deviceId,
                        remoteTarget,
                        result.offsetMs * 1_000_000L,
                        result.rttMs
                    ).encode()
                    runCatching { socket?.send(DatagramPacket(commit, commit.size, datagram.address, datagram.port)) }
                        .onFailure {
                            publishPeer(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                            clearPeerOperation()
                        }
                    publishPeer(control.deviceId, PeerCalibrationState(PeerCalibrationPhase.WAITING_TARGET, result.offsetMs, result.rttMs))
                }
            }
            PeerCalibrationControl.REJECT, PeerCalibrationControl.CANCEL -> {
                if (control.operation == peerOperation) {
                    publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.FAILED))
                    clearPeerOperation()
                }
            }
            PeerCalibrationControl.COMMIT -> {
                if (control.pcId != pc.deviceId || control.operation != peerOperation ||
                    control.deviceId != peerDeviceId || datagram.address != lastPeerAddress ||
                    datagram.port != lastPeerPort
                ) return

                peerTargetLocalNs = control.targetNs
                peerOffsetMs = -control.offsetNs / 1_000_000L
                peerRttMs = control.rttMs
                publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.WAITING_TARGET, peerOffsetMs, peerRttMs))
            }
            PeerCalibrationControl.COMPLETE -> {
                if (control.operation == peerOperation && control.deviceId == peerDeviceId) {
                    publishPeer(peerDeviceId, PeerCalibrationState(PeerCalibrationPhase.COMPLETE, peerOffsetMs, peerRttMs))
                }
            }
        }
    }
}
