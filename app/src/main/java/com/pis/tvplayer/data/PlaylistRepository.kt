package com.pis.tvplayer.data

import android.content.Context
import android.util.Log
import com.pis.tvplayer.data.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches the playlist JSON from the remote URL, caching the last successful payload to
 * disk. When the network is unavailable the cached copy is returned so the player keeps
 * running offline (satisfies offline playback of previously-seen content).
 */
class PlaylistRepository(
    context: Context,
    private val config: RemoteConfig
) {

    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, CACHE_FILENAME)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    sealed interface Result {
        data class Live(val playlist: Playlist) : Result
        data class Cached(val playlist: Playlist) : Result
        data class Empty(val reason: String) : Result
    }

    /** Fetch the latest playlist, falling back to disk cache on failure. */
    suspend fun load(): Result = withContext(Dispatchers.IO) {
        val url = config.playlistUrl
        try {
            val body = fetch(url)
            val playlist = json.decodeFromString<Playlist>(body)
            writeCache(body)
            Log.i(TAG, "Loaded live playlist: ${playlist.items.size} item(s)")
            Result.Live(playlist)
        } catch (t: Throwable) {
            Log.w(TAG, "Live fetch failed ($url): ${t.message}. Falling back to cache.")
            loadFromCache()
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            return response.body?.string()
                ?: throw IllegalStateException("Empty response body")
        }
    }

    private fun loadFromCache(): Result {
        if (!cacheFile.exists()) {
            return Result.Empty("No network and no cached playlist")
        }
        return try {
            val playlist = json.decodeFromString<Playlist>(cacheFile.readText())
            Result.Cached(playlist)
        } catch (t: Throwable) {
            Result.Empty("Cached playlist unreadable: ${t.message}")
        }
    }

    private fun writeCache(raw: String) {
        try {
            cacheFile.writeText(raw)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to write playlist cache: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "PlaylistRepository"
        private const val CACHE_FILENAME = "playlist_cache.json"
    }
}
