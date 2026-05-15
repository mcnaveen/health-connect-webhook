package com.hcwebhook.app.releases

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ReleaseRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getReleases(forceRefresh: Boolean = false): List<GithubRelease> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            readCache()?.let { return@withContext it }
        }
        fetchFromNetwork()?.also { writeCache(it) } ?: readCache().orEmpty()
    }

    suspend fun findReleaseForVersion(versionName: String): GithubRelease? {
        val releases = getReleases()
        return releases.firstOrNull { ReleaseNotesFormatter.matchesVersion(it.tagName, versionName) }
            ?: releases.firstOrNull()
    }

    private fun fetchFromNetwork(): List<GithubRelease>? {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                json.decodeFromString<List<GithubRelease>>(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readCache(): List<GithubRelease>? {
        val raw = prefs.getString(KEY_CACHE, null) ?: return null
        val ts = prefs.getLong(KEY_CACHE_TS, 0L)
        if (System.currentTimeMillis() - ts > CACHE_TTL_MS) return null
        return try {
            json.decodeFromString<List<GithubRelease>>(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(releases: List<GithubRelease>) {
        prefs.edit()
            .putString(KEY_CACHE, json.encodeToString(releases))
            .putLong(KEY_CACHE_TS, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "hc_releases_cache"
        private const val KEY_CACHE = "releases"
        private const val KEY_CACHE_TS = "ts"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private const val RELEASES_URL =
            "https://api.github.com/repos/mcnaveen/health-connect-webhook/releases?per_page=20"
    }
}
