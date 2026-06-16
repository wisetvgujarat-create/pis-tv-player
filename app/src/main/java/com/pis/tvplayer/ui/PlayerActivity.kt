package com.pis.tvplayer.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import com.pis.tvplayer.R
import com.pis.tvplayer.databinding.ActivityPlayerBinding
import com.pis.tvplayer.player.PlayerManager
import kotlinx.coroutines.launch

/**
 * Single fullscreen kiosk activity. Loads the playlist, builds a looping concatenated
 * media source, and plays it edge-to-edge with no controls. Playback errors skip to the
 * next item instead of crashing.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var playerManager: PlayerManager
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        playerManager = PlayerManager(this)
        enterImmersiveMode()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadPlaylist()
    }

    private fun render(state: PlayerViewModel.UiState) {
        when (state) {
            is PlayerViewModel.UiState.Loading -> {
                showStatus(getString(R.string.loading))
            }

            is PlayerViewModel.UiState.Ready -> {
                if (state.fromCache) {
                    Log.i(TAG, "Playing cached playlist (offline)")
                }
                startPlayback(state)
            }

            is PlayerViewModel.UiState.Empty -> {
                Log.w(TAG, "No content: ${state.reason}")
                showStatus(getString(R.string.no_content))
            }
        }
    }

    private fun startPlayback(state: PlayerViewModel.UiState.Ready) {
        releasePlayer()
        val exo = playerManager.build()
        player = exo
        binding.playerView.player = exo

        val concat = ConcatenatingMediaSource(
            *state.playlist.items
                .map(playerManager::toMediaSource)
                .toTypedArray()
        )
        exo.repeatMode =
            if (state.playlist.loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        exo.addListener(playerListener)
        exo.setMediaSource(concat)
        exo.prepare()
        exo.play()

        binding.statusText.visibility = View.GONE
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            // Skip the failing item rather than crash; keep the loop alive.
            val exo = player ?: return
            Log.w(TAG, "Playback error: ${error.errorCodeName}. Skipping item.")
            if (exo.hasNextMediaItem()) {
                exo.seekToNextMediaItem()
                exo.prepare()
                exo.play()
            } else {
                exo.seekTo(0, 0L)
                exo.prepare()
                exo.play()
            }
        }
    }

    private fun showStatus(text: String) {
        binding.statusText.text = text
        binding.statusText.visibility = View.VISIBLE
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let {
            it.removeListener(playerListener)
            it.release()
        }
        player = null
        binding.playerView.player = null
    }

    companion object {
        private const val TAG = "PlayerActivity"
    }
}
