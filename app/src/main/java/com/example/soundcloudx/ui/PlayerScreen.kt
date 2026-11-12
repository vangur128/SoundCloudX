package com.example.soundcloudx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.soundcloudx.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(playerViewModel: PlayerViewModel, onBackClick: () -> Unit) {
    val track = playerViewModel.currentTrack ?: return
    val context = LocalContext.current
    val isPlaying = playerViewModel.isPlaying

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playerViewModel.updatePosition()
            delay(1000)
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 15000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF1A1A1A), Color(0xFF0A0A0A)))).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        
        AsyncImage(
            model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)).rotate(rotation).background(Color.DarkGray)
        )

        Spacer(Modifier.height(48.dp))
        Text(track.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Text(track.user.username, fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        Slider(
            value = playerViewModel.currentPosition.toFloat(),
            onValueChange = { playerViewModel.seekTo(it.toLong()) },
            valueRange = 0f..playerViewModel.duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF1DB954), activeTrackColor = Color(0xFF1DB954), inactiveTrackColor = Color(0xFF333333)),
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playerViewModel.currentPosition), color = Color.Gray, fontSize = 12.sp)
            Text(formatDuration(playerViewModel.duration), color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { playerViewModel.previousTrack(context) }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            IconButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF1DB954))
            ) {
                Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(56.dp))
            }
            IconButton(onClick = { playerViewModel.nextTrack(context) }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
