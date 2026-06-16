package com.pis.tvplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.pis.tvplayer.data.model.MediaType
import com.pis.tvplayer.data.model.PlaylistItem

/** Builds and owns the ExoPlayer instance with cache-backed, HLS-capable sources. */
class PlayerManager(private val context: Context) {

    private val dataSourceFactory = MediaCache.dataSourceFactory(context)

    fun build(): ExoPlayer {
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                // Loop handled at playlist level; keep individual items single-shot.
                playWhenReady = true
            }
    }

    /** Maps a playlist item to a cache-backed MediaSource (progressive or HLS). */
    fun toMediaSource(item: PlaylistItem): MediaSource {
        val mediaItem = MediaItem.Builder()
            .setUri(item.url)
            .apply {
                if (item.type == MediaType.HLS) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()

        return when (item.type) {
            MediaType.HLS ->
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            MediaType.VIDEO ->
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }
}
