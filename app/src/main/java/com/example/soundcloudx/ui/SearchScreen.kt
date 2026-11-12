package com.example.soundcloudx.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.soundcloudx.PlayerViewModel
import com.example.soundcloudx.sc.SoundCloudApi
import com.example.soundcloudx.sc.SCTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(playerViewModel: PlayerViewModel, context: Context) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SCTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showPlayer && playerViewModel.currentTrack != null) {
        PlayerScreenView(playerViewModel, context) { showPlayer = false }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0A0A0A), Color(0xFF1A1A1A))))
    ) {
        Text("SoundCloudX", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954), modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp))
        Text("Найди свою музыку", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(24.dp, 0.dp, 24.dp, 16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Название трека или артист", color = Color.Gray) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF1DB954)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color(0xFF333333),
                focusedLabelColor = Color(0xFF1DB954),
                cursorColor = Color(0xFF1DB954)
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (query.isBlank()) return@Button
                isLoading = true
                scope.launch {
                    results = try { SoundCloudApi.search(query, limit = 50) } catch (e: Exception) { emptyList() }
                    isLoading = false
                }
            },
            enabled = !isLoading && query.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isLoading) "Поиск..." else "Найти треки", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            items(results) { track ->
                TrackRow(track, playerViewModel, context) { showPlayer = true }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }

    playerViewModel.currentTrack?.let { track ->
        if (!showPlayer) {
            NowPlayingBar(track, playerViewModel) { showPlayer = true }
        }
    }
}

@Composable
fun TrackRow(track: SCTrack, playerViewModel: PlayerViewModel, context: Context, onTrackClick: () -> Unit) {
    val isPlaying = playerViewModel.currentTrack?.id == track.id && playerViewModel.isPlaying

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onTrackClick() }.background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(track.user.username, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
        }
        IconButton(
            onClick = { if (isPlaying) playerViewModel.togglePlayPause() else playerViewModel.playTrack(track, context) },
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF1DB954))
        ) {
            Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun NowPlayingBar(track: SCTrack, playerViewModel: PlayerViewModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF282828)).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
        Column(modifier = Modifier.weight(1f)) {
            Text("Сейчас играет", color = Color(0xFF1DB954), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { playerViewModel.togglePlayPause() }) {
            Icon(imageVector = if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Toggle", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun PlayerScreenView(playerViewModel: PlayerViewModel, context: Context, onBackClick: () -> Unit) {
    val track = playerViewModel.currentTrack ?: return
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Сейчас играет", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        
        Spacer(Modifier.height(24.dp))
        
        AsyncImage(
            model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)).rotate(rotation).background(Color.DarkGray)
        )

        Spacer(Modifier.height(32.dp))
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
            IconButton(onClick = { }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.Gray, modifier = Modifier.size(48.dp))
            }
            IconButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF1DB954))
            ) {
                Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(56.dp))
            }
            IconButton(onClick = { }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.Gray, modifier = Modifier.size(48.dp))
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
