package com.haoze.claudekeyboard.ui.compose

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class RecognitionMember(
    val name: String,
    val acknowledgement: String,
    val avatarFileName: String?
)

data class RecognitionMembersConfiguration(
    val sponsors: List<RecognitionMember>,
    val coBuilders: List<RecognitionMember>
)

object RecognitionMembersRepository {
    private const val CONFIGURATION_URL =
        "https://raw.githubusercontent.com/haoze-evolluling/SyncTouch/main/recognition_members.json"
    private const val CACHE_FILE_NAME = "recognition_members.json"
    private const val PREFERENCES_NAME = "recognition_members"
    private const val ETAG_KEY = "etag"
    private const val MAX_CONFIGURATION_BYTES = 256 * 1024L
    private val avatarFileNamePattern = Regex("[a-z0-9_]+_avatar\\.jpg")
    private val refreshMutex = Mutex()

    private const val DEFAULT_CONFIGURATION_JSON = """{
  "version": 1,
  "sponsors": [
    { "name": "酷嘎法" }
  ],
  "coBuilders": []
}"""

    suspend fun loadCached(context: Context): RecognitionMembersConfiguration? =
        withContext(Dispatchers.IO) {
            readConfiguration(cacheFile(context)) ?: parseConfiguration(DEFAULT_CONFIGURATION_JSON)
        }

    /** Returns a configuration only when the server provided a changed, valid document. */
    suspend fun refresh(context: Context): RecognitionMembersConfiguration? =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                val url = URL(CONFIGURATION_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    preferences.getString(ETAG_KEY, null)?.let { setRequestProperty("If-None-Match", it) }
                }

                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) return@withLock null
                    if (responseCode !in 200..299) throw IOException("HTTP ")

                    val bytes = connection.inputStream.use { input ->
                        input.readBytesLimited(MAX_CONFIGURATION_BYTES)
                    }
                    val json = bytes.toString(Charsets.UTF_8)
                    val configuration = parseConfiguration(json)
                    writeCache(cacheFile(context), json)
                    preferences.edit().putString(ETAG_KEY, connection.getHeaderField("ETag")).apply()
                    configuration
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun readConfiguration(file: File): RecognitionMembersConfiguration? =
        try {
            file.takeIf { it.isFile }?.readText()?.let(::parseConfiguration)
        } catch (_: Exception) {
            null
        }

    private fun parseConfiguration(json: String): RecognitionMembersConfiguration {
        val root = JSONObject(json)
        require(root.has("version") && !root.isNull("version")) { "名单配置缺少版本" }
        return RecognitionMembersConfiguration(
            sponsors = parseMembers(root.getJSONArray("sponsors"), "感谢您对 SyncTouch 项目的赞助支持"),
            coBuilders = parseMembers(root.getJSONArray("coBuilders"), "感谢为 SyncTouch 提出建议与帮助测试")
        ).also { configuration ->
            validateUniqueMembers(configuration.sponsors)
            validateUniqueMembers(configuration.coBuilders)
        }
    }

    private fun validateUniqueMembers(members: List<RecognitionMember>) {
        require(members.map(RecognitionMember::name).toSet().size == members.size) { "名单存在重复成员" }
        val remoteAvatarFileNames = members.mapNotNull(RecognitionMember::avatarFileName)
        require(remoteAvatarFileNames.toSet().size == remoteAvatarFileNames.size) { "名单存在重复头像" }
    }

    private fun parseMembers(members: JSONArray, acknowledgement: String): List<RecognitionMember> =
        List(members.length()) { index ->
            val member = members.getJSONObject(index)
            val name = member.getString("name").trim()
            val avatarFileName = member.optString("avatarFileName").trim().takeIf(String::isNotEmpty)
            require(name.isNotEmpty()) { "成员名称不能为空" }
            require(
                avatarFileName == null || avatarFileName.matches(avatarFileNamePattern)
            ) { "头像文件名无效" }
            RecognitionMember(
                name = name,
                acknowledgement = acknowledgement,
                avatarFileName = avatarFileName
            )
        }

    private fun cacheFile(context: Context): File = File(context.filesDir, CACHE_FILE_NAME)

    private fun writeCache(file: File, contents: String) {
        val atomicFile = AtomicFile(file)
        val output = atomicFile.startWrite()
        try {
            output.write(contents.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: IOException) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun java.io.InputStream.readBytesLimited(maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val count = read(buffer)
            if (count == -1) break
            totalBytes += count
            if (totalBytes > maxBytes) throw IOException("名单配置过大")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
