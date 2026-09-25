package com.haoze.claudekeyboard.audio

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pcTrustDataStore by preferencesDataStore(name = "steamvoice_pc_trust")

/**
 * 记住用户选择“以后自动同意”的电脑，按稳定设备标识存储（条目格式 "id\u0000名称"）。
 */
class PcTrustRepository(private val context: Context) {

    private object Keys {
        val entries = stringSetPreferencesKey("authorized_pcs")
    }

    /** 设备标识 → 名称 的已信任映射。 */
    val trusted: Flow<Map<String, String>> = context.pcTrustDataStore.data.map { prefs ->
        (prefs[Keys.entries] ?: emptySet()).mapNotNull(::parseEntry).toMap()
    }

    suspend fun isTrusted(deviceId: String): Boolean = trusted.first().containsKey(deviceId)

    suspend fun nameOf(deviceId: String): String? = trusted.first()[deviceId]

    suspend fun trust(deviceId: String, name: String) {
        if (deviceId.isEmpty()) return
        context.pcTrustDataStore.edit { prefs ->
            val kept = (prefs[Keys.entries] ?: emptySet()).filterNot { parseEntry(it)?.first == deviceId }
            prefs[Keys.entries] = kept.toSet() + formatEntry(deviceId, name)
        }
    }

    suspend fun untrust(deviceId: String) {
        context.pcTrustDataStore.edit { prefs ->
            val kept = (prefs[Keys.entries] ?: emptySet()).filterNot { parseEntry(it)?.first == deviceId }
            prefs[Keys.entries] = kept.toSet()
        }
    }

    private fun formatEntry(deviceId: String, name: String): String = "$deviceId\u0000$name"

    private fun parseEntry(entry: String): Pair<String, String>? {
        val idx = entry.indexOf('\u0000')
        if (idx <= 0 || idx == entry.length - 1) return null
        return entry.substring(0, idx) to entry.substring(idx + 1)
    }
}

