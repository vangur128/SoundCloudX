package com.example.soundcloudx.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.soundcloudx.PlayerViewModel
import com.example.soundcloudx.sc.SoundCloudApi
import com.example.soundcloudx.sc.SCTrack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(playerViewModel: PlayerViewModel, context: Context) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SCTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showPlayer && playerViewModel.currentTrack != null) {
        Column(Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showPlayer = false }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text("Сейчас играет", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            AsyncImage(model = playerViewModel.currentTrack!!.artworkUrl, contentDescription = null, modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray))
            Spacer(Modifier.height(24.dp))
            Text(playerViewModel.currentTrack!!.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(playerViewModel.currentTrack!!.user.username, color = Color.Gray, fontSize = 16.sp)
            Spacer(Modifier.height(32.dp))
            Slider(value = playerViewModel.currentPosition.toFloat(), onValueChange = { playerViewModel.seekTo(it.toLong()) }, valueRange = 0f..playerViewModel.duration.toFloat().coerceAtLeast(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF1DB954), activeTrackColor = Color(0xFF1DB954), inactiveTrackColor = Color(0xFF333333)), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(playerViewModel.currentPosition), color = Color.Gray, fontSize = 12.sp)
                Text(formatDuration(playerViewModel.duration), color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playerViewModel.togglePlayPause() }, modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF1DB954))) {
                    Icon(if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(48.dp))
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF1A1A1A))))) {
        Text("Melo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954), modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Поиск...", color = Color.Gray) }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF1DB954)) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1DB954), unfocusedBorderColor = Color(0xFF333333)), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (query.isBlank()) return@Button
            isLoading = true
            errorMessage = null
            scope.launch {
                try {
                    val found = SoundCloudApi.search(query, limit = 50)
                    if (found.isEmpty()) errorMessage = "Ничего не найдено."
                    results = found
                } catch (e: Exception) {
                    errorMessage = "Ошибка: ${e.message}"
                    results = emptyList()
                } finally {
                    isLoading = false
                }
            }
        }, enabled = !isLoading && query.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954), contentColor = Color.Black), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp), shape = RoundedCornerShape(16.dp)) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 3.dp) else Text("Найти", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        errorMessage?.let {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF331111))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFFF6B6B))
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 14.sp, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            items(results) { track ->
                Row(modifier = Modifier.fillMaxWidth().clickable { showPlayer = true }.background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.user.username, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                    }
                    IconButton(onClick = { if (playerViewModel.currentTrack?.id == track.id && playerViewModel.isPlaying) playerViewModel.togglePlayPause() else playerViewModel.playTrack(track, context) }, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF1DB954))) {
                        Icon(if (playerViewModel.currentTrack?.id == track.id && playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(28.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
    playerViewModel.currentTrack?.let { track ->
        if (!showPlayer) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF282828)).clickable { showPlayer = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Сейчас играет", color = Color(0xFF1DB954), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                    Icon(if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Toggle", tint = Color.White, modifier = Modifier.size(32.dp))
                }
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
