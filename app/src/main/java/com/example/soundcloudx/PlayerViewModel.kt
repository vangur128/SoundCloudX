package com.example.soundcloudx

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.soundcloudx.sc.SoundCloudApi
import com.example.soundcloudx.sc.SCTrack
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    var exoPlayer: ExoPlayer? = null
    var currentTrack by mutableStateOf<SCTrack?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var queue by mutableStateOf<List<SCTrack>>(emptyList())
    var queueIndex by mutableStateOf(0)

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
            exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    this@PlayerViewModel.isPlaying = isPlaying
                }
                
                override fun onPositionDiscontinuity(
                    oldPosition: androidx.media3.common.Player.PositionInfo,
                    newPosition: androidx.media3.common.Player.PositionInfo,
                    reason: Int
                ) {
                    updatePosition()
                }
            })
        }
    }

    fun playTrack(track: SCTrack, context: Context, trackList: List<SCTrack> = emptyList()) {
        viewModelScope.launch {
            try {
                currentTrack = track
                queue = trackList
                queueIndex = trackList.indexOf(track).coerceAtLeast(0)
                
                initPlayer(context)
                val streamUrl = SoundCloudApi.getStreamUrl(track)
                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying) {
            exoPlayer?.pause()
            isPlaying = false
        } else {
            exoPlayer?.play()
            isPlaying = true
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition = position
    }

    fun nextTrack(context: Context) {
        if (queueIndex < queue.size - 1) {
            queueIndex++
            playTrack(queue[queueIndex], context, queue)
        }
    }

    fun previousTrack(context: Context) {
        if (queueIndex > 0) {
            queueIndex--
            playTrack(queue[queueIndex], context, queue)
        }
    }

    fun updatePosition() {
        currentPosition = exoPlayer?.currentPosition ?: 0L
        duration = exoPlayer?.duration ?: 0L
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
