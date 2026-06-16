package com.pis.tvplayer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote playlist contract. Example JSON:
 *
 * {
 *   "version": 1,
 *   "loop": true,
 *   "items": [
 *     { "url": "https://cdn/clip1.mp4", "type": "video" },
 *     { "url": "https://cdn/stream.m3u8", "type": "hls" }
 *   ]
 * }
 */
@Serializable
data class Playlist(
    val version: Int = 1,
    val loop: Boolean = true,
    val items: List<PlaylistItem> = emptyList()
)

@Serializable
data class PlaylistItem(
    val url: String,
    val type: MediaType = MediaType.VIDEO,
    /** Optional id for logging / future remote-command targeting. */
    val id: String? = null
)

@Serializable
enum class MediaType {
    @SerialName("video")
    VIDEO,

    @SerialName("hls")
    HLS
}
