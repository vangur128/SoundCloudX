package com.example.soundcloudx.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1025)).padding(16.dp)) {
        Text("Melo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9A8E8), modifier = Modifier.padding(bottom = 16.dp))
        
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Введите запрос...", color = Color.Gray) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9B7CB6)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF9B7CB6),
                unfocusedBorderColor = Color(0xFF3D2E4A)
            ),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        )
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = {
                if (query.isBlank()) return@Button
                isLoading = true
                errorMessage = null
                scope.launch {
                    try {
                        val found = SoundCloudApi.search(query, limit = 30)
                        if (found.isEmpty()) {
                            errorMessage = "Ничего не найдено. Попробуй другой запрос."
                        }
                        results = found
                    } catch (e: Throwable) {
                        errorMessage = "Ошибка: ${e.toString()}"
                        results = emptyList()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && query.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B7CB6), contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            else Text("Найти", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        errorMessage?.let { error ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF331133))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFFF6B9B))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = Color(0xFFFF6B9B), modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { track ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { playerViewModel.playTrack(track, context) }
                        .background(Color(0xFF2A1F35), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF3D2E4A)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.user.username, color = Color(0xFF9B7CB6), fontSize = 12.sp, maxLines = 1)
                    }
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color(0xFFC9A8E8), modifier = Modifier.size(32.dp))
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    playerViewModel.currentTrack?.let { track ->
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF2A1F35)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF3D2E4A)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Играет:", color = Color(0xFFC9A8E8), fontSize = 10.sp)
                Text(track.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                Icon(if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Toggle", tint = Color(0xFFC9A8E8))
            }
        }
    }
}
