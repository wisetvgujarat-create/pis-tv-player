package com.pis.tvplayer.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide ExoPlayer download cache. SimpleCache permits only one instance per
 * directory per process, so it is created lazily and shared.
 */
object MediaCache {

    private const val CACHE_DIR = "media_cache"
    private const val MAX_BYTES = 512L * 1024 * 1024 // 512 MB

    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: build(context.applicationContext).also { cache = it }
        }
    }

    private fun build(context: Context): SimpleCache {
        val dir = File(context.cacheDir, CACHE_DIR)
        return SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context)
        )
    }

    /** Builds a cache-backed data source factory: serves from disk, fills from network. */
    fun dataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstream = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        return CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
