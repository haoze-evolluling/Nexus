package com.haoze.nexus.audio

import android.content.Context
import android.media.session.MediaSession
import android.media.session.PlaybackState

/**
 * Manages the Android [MediaSession] lifecycle and playback state for lock screen controls.
 */
class AudioMediaSessionManager(private val context: Context, private val tag: String) {

    private var mediaSession: MediaSession? = null

    fun ensureMediaSession() {
        if (mediaSession != null) return
        mediaSession = MediaSession(context, tag).also { session ->
            session.isActive = true
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                    .setState(PlaybackState.STATE_PAUSED, 0L, 0f)
                    .build()
            )
        }
    }

    fun updatePlaybackState(playing: Boolean) {
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                .setState(state, 0L, if (playing) 1f else 0f)
                .build()
        )
    }

    fun release() {
        mediaSession?.release()
        mediaSession = null
    }
}
