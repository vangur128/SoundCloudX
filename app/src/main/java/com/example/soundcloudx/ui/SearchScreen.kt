package com.example.soundcloudx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.soundcloudx.PlayerViewModel
import com.example.soundcloudx.sc.SoundCloudApi
import com.example.soundcloudx.sc.SCTrack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(playerViewModel: PlayerViewModel) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SCTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // Верхняя часть с поиском и списком (занимает всё свободное место)
        Column(Modifier.weight(1f).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск треков", color = Color.Gray) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color(0xFF1DB954)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = {
                    if (query.isBlank()) return@Button
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            results = SoundCloudApi.search(query)
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Ошибка сети"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && query.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { 
                Text(if (isLoading) "Ищем..." else "Найти", color = Color.Black, style = MaterialTheme.typography.titleMedium) 
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(results) { track ->
                    TrackRow(track, playerViewModel, context)
                }
            }
        }

        // Нижняя панель "Сейчас играет" (всегда внизу)
        playerViewModel.currentTrack?.let { track ->
            NowPlayingBar(track, playerViewModel)
        }
    }
}

@Composable
fun TrackRow(track: SCTrack, playerViewModel: PlayerViewModel, context: android.content.Context) {
    val isCurrentlyPlaying = playerViewModel.currentTrack?.id == track.id && playerViewModel.isPlaying
    
    Row(
        Modifier.fillMaxWidth().clickable { 
            playerViewModel.playTrack(track, context) 
        }.background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.medium).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl, 
            contentDescription = null, 
            modifier = Modifier.size(56.dp).background(Color.DarkGray, shape = MaterialTheme.shapes.small)
        )
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.user.username, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        IconButton(onClick = { 
            if (isCurrentlyPlaying) {
                playerViewModel.togglePlayPause()
            } else {
                playerViewModel.playTrack(track, context)
            }
        }) {
            Icon(
                imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun NowPlayingBar(track: SCTrack, playerViewModel: PlayerViewModel) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF282828)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl, 
            contentDescription = null, 
            modifier = Modifier.size(48.dp).background(Color.DarkGray, shape = MaterialTheme.shapes.small)
        )
        Column(Modifier.weight(1f)) {
            Text("Сейчас играет", color = Color(0xFF1DB954), style = MaterialTheme.typography.labelSmall)
            Text(track.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { playerViewModel.togglePlayPause() }) {
            Icon(
                imageVector = if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Toggle Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
