package com.haoze.claudekeyboard.audio

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val VALID_BITRATES = setOf(64, 96, 128, 192)

data class AudioSettings(val initialBitrateKbps: Int = 128, val frameMs: Int = 10, val updatedAtMs: Long = 0L, val deviceId: String = "")

private val Context.settingsDataStore by preferencesDataStore(name = "steamvoice_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val bitrate = intPreferencesKey("opus_bitrate_kbps")
        val frameMs = intPreferencesKey("audio_frame_ms")
        val updatedAtMs = androidx.datastore.preferences.core.longPreferencesKey("settings_updated_at_ms")
        val deviceId = androidx.datastore.preferences.core.stringPreferencesKey("settings_device_id")
    }

    val settings: Flow<AudioSettings> = context.settingsDataStore.data.map { prefs: Preferences ->
        val bitrate = (prefs[Keys.bitrate] ?: 128).takeIf { it in VALID_BITRATES } ?: 128
        val frame = (prefs[Keys.frameMs] ?: 10).takeIf { it == 10 || it == 20 } ?: 10
        // The device identity must never be accepted from a peer's settings
        // packet. Older releases persisted that field from the desktop and
        // caused every receiver connected to one PC to advertise the same ID.
        AudioSettings(bitrate, frame, prefs[Keys.updatedAtMs] ?: 0L, defaultDeviceId(context))
    }

    suspend fun setInitialBitrate(kbps: Int) = update(kbps, settingsSnapshot().frameMs)
    suspend fun setFrameMs(frameMs: Int) = update(settingsSnapshot().initialBitrateKbps, frameMs)
    suspend fun applyIfNewer(incoming: AudioSettings): Boolean {
        val current = settingsSnapshot()
        if (compareVersion(incoming, current) <= 0) return false
        update(incoming.initialBitrateKbps, incoming.frameMs, incoming.updatedAtMs)
        return true
    }
    private suspend fun settingsSnapshot() = settings.first()
    private suspend fun update(kbps: Int, frameMs: Int, timestamp: Long = System.currentTimeMillis()) {
        require(kbps in VALID_BITRATES)
        require(frameMs == 10 || frameMs == 20)
        context.settingsDataStore.edit { it[Keys.bitrate] = kbps; it[Keys.frameMs] = frameMs; it[Keys.updatedAtMs] = timestamp; it[Keys.deviceId] = defaultDeviceId(context) }
    }
}

private fun defaultDeviceId(context: Context): String = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "android"
private fun compareVersion(a: AudioSettings, b: AudioSettings): Int = compareValuesBy(a, b, { it.updatedAtMs }, { it.deviceId })

