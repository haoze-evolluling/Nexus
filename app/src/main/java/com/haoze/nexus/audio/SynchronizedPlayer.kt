package com.haoze.claudekeyboard.audio

import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

/**
 * 按目标播放时间调度解码帧的播放器，实现多设备间同步外放。
 *
 * 每帧的目标播放时刻 = mapToLocal(tsNs) + latencyBudgetNs（本机单调时钟）。
 * 启动阶段等待首帧目标时刻再开始写入 AudioTrack，使多台设备在相同的
 * 目标时刻开始出声；此后依靠阻塞写自身节流。本机声卡与发送端时钟的
 * 长期漂移通过微调 playbackRate（±0.2% 内，听感无差别）平滑吸收，
 * 校准过程不产生跳音或爆音。
 */
class SynchronizedPlayer(
    private var track: AudioTrack,
    private val trackFactory: (() -> AudioTrack)? = null,
    val latencyBudgetNs: Long = 150_000_000L,
    private val nowNs: () -> Long = System::nanoTime,
) {
    class Frame(val pcm: ByteArray, val tsNs: Long)

    private val queue = ArrayBlockingQueue<Frame>(QUEUE_CAPACITY)

    @Volatile private var closed = false
    @Volatile private var mapToLocal: ((Long) -> Long?)? = null
    private var worker: Thread? = null

    // 漂移控制状态仅播放线程访问（playedFrames 除外，供接收线程只读）。
    private var latenessEmaNs = 0.0
    private var playbackRate = NOMINAL_RATE
    private var nextRateCheckNs = 0L
    var droppedLateFrames = 0L
        private set

    /** 已写入 AudioTrack 的帧数；接收线程读取以判定校准是否完成。 */
    @Volatile var playedFrames = 0L
        private set

    fun start() {
        if (worker != null) return
        worker = thread(name = "steamvoice-player") { runLoop() }
    }

    /** 安装时钟映射（发送端流时钟 → 本机时钟）；时钟未收敛时返回 null。 */
    fun setClock(fn: (Long) -> Long?) {
        mapToLocal = fn
    }

    /** 接收线程投递解码帧；队列满返回 false（发送端远快于播放时丢弃）。 */
    fun offer(pcm: ByteArray, tsNs: Long): Boolean = queue.offer(Frame(pcm, tsNs))

    fun backlogFrames(): Int = queue.size

    fun close() {
        closed = true
        worker?.interrupt()
    }

    /** Drop frames from the previous connection while keeping the worker alive. */
    fun resetForSession() {
        queue.clear()
        playedFrames = 0L
        latenessEmaNs = 0.0
        playbackRate = NOMINAL_RATE
        nextRateCheckNs = 0L
        runCatching {
            track.pause()
            track.flush()
            track.play()
        }.onFailure { Log.w(TAG, "AudioTrack reset failed", it) }
    }

    private fun runLoop() {
        while (!closed && !Thread.currentThread().isInterrupted) {
            val head = queue.peek()
            if (head == null) {
                sleepQuiet(5)
                continue
            }
            val mapper = mapToLocal
            if (mapper == null) {
                // 时钟尚未收敛：短暂等待，队列上限兜底防止无限积压。
                sleepQuiet(20)
                continue
            }
            val target = mapper(head.tsNs)
            if (target == null) {
                sleepQuiet(20)
                continue
            }
            val waitNs = target + latencyBudgetNs - nowNs()
            if (waitNs > WAIT_GRANULARITY_NS) {
                val sleepMs = (min(waitNs / 2, 20_000_000L) / 1_000_000L) + 1
                sleepQuiet(sleepMs)
                continue
            }
            queue.poll()
            if (waitNs < -LATE_DROP_NS) {
                // 严重迟到（网络中断后恢复等）：丢弃追齐时间线。
                droppedLateFrames++
                continue
            }
            val lateness = (-waitNs).coerceAtLeast(0)
            latenessEmaNs = latenessEmaNs * EMA_ALPHA + lateness * (1 - EMA_ALPHA)
            adjustRateIfNeeded()
            val written = track.write(head.pcm, 0, head.pcm.size, AudioTrack.WRITE_BLOCKING)
            if (written < 0) {
                Log.e(TAG, "AudioTrack.write failed result=$written state=${track.state} playState=${track.playState}")
                val factory = trackFactory
                if (written == AudioTrack.ERROR_DEAD_OBJECT && factory != null) {
                    val old = track
                    val replacement = runCatching { factory.invoke() }.getOrNull()
                    if (replacement != null) {
                        track = replacement
                        runCatching { old.release() }
                        Log.i(TAG, "AudioTrack recreated after dead object")
                        continue
                    }
                }
                break
            }
            playedFrames++
        }
    }

    /**
     * 周期校准：持续偏晚说明本机声卡消耗慢于发送端时间线，微升速率；
     * 回到容差内则恢复标称 48 kHz。步长与上限都很小，听感无差别。
     */
    private fun adjustRateIfNeeded() {
        val now = nowNs()
        if (now < nextRateCheckNs) return
        nextRateCheckNs = now + RATE_CHECK_INTERVAL_NS
        val ema = latenessEmaNs.toLong()
        when {
            ema > LATE_THRESHOLD_NS && playbackRate < MAX_RATE -> {
                playbackRate += RATE_STEP_HZ
                applyRate()
            }
            ema < -LATE_THRESHOLD_NS && playbackRate > MIN_RATE -> {
                playbackRate -= RATE_STEP_HZ
                applyRate()
            }
            abs(ema) < RESET_THRESHOLD_NS && playbackRate != NOMINAL_RATE -> {
                playbackRate = NOMINAL_RATE
                applyRate()
            }
        }
    }

    private fun applyRate() {
        runCatching { track.playbackRate = playbackRate }
    }

    private fun sleepQuiet(ms: Long) {
        runCatching { Thread.sleep(ms) }
    }

    private companion object {
        const val TAG = "SteamVoicePlayer"
        const val QUEUE_CAPACITY = 256
        const val NOMINAL_RATE = 48000
        const val RATE_STEP_HZ = 24
        const val MAX_RATE = 48096
        const val MIN_RATE = 47904
        const val WAIT_GRANULARITY_NS = 250_000L
        const val LATE_DROP_NS = 150_000_000L
        const val LATE_THRESHOLD_NS = 30_000_000L
        const val RESET_THRESHOLD_NS = 10_000_000L
        const val RATE_CHECK_INTERVAL_NS = 3_000_000_000L
        const val EMA_ALPHA = 0.95
    }
}

