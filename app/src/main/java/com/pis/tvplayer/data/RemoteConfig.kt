package com.pis.tvplayer.data

import android.content.Context
import androidx.core.content.edit
import com.pis.tvplayer.BuildConfig

/**
 * Holds the playlist source URL and poll interval. Defaults come from BuildConfig but
 * can be overridden at runtime (e.g. a future provisioning screen or remote command)
 * via SharedPreferences without rebuilding the APK.
 */
class RemoteConfig(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val playlistUrl: String
        get() = prefs.getString(KEY_PLAYLIST_URL, null)
            ?: BuildConfig.DEFAULT_PLAYLIST_URL

    /** Re-fetch interval in ms. 0 means fetch once on startup only. */
    val pollIntervalMs: Long
        get() = prefs.getLong(KEY_POLL_INTERVAL, BuildConfig.PLAYLIST_POLL_INTERVAL_MS)

    fun setPlaylistUrl(url: String) {
        prefs.edit { putString(KEY_PLAYLIST_URL, url) }
    }

    fun setPollIntervalMs(intervalMs: Long) {
        prefs.edit { putLong(KEY_POLL_INTERVAL, intervalMs) }
    }

    companion object {
        private const val PREFS_NAME = "tvplayer_config"
        private const val KEY_PLAYLIST_URL = "playlist_url"
        private const val KEY_POLL_INTERVAL = "poll_interval_ms"
    }
}
