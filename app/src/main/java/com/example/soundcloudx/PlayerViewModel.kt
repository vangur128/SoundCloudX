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

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
    }

    fun playTrack(track: SCTrack, context: Context) {
        viewModelScope.launch {
            try {
                currentTrack = track
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

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
