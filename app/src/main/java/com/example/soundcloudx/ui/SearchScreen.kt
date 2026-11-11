package com.example.soundcloudx.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.soundcloudx.sc.SoundCloudApi
import com.example.soundcloudx.sc.SCTrack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SCTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск треков") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (query.isBlank()) return@Button
                isLoading = true
                errorMessage = null
                scope.launch {
                    try {
                        results = SoundCloudApi.search(query)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Ошибка"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && query.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isLoading) "Загрузка..." else "Найти") }

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { track ->
                TrackRow(track)
            }
        }
    }
}

@Composable
fun TrackRow(track: SCTrack) {
    Row(
        Modifier.fillMaxWidth().clickable { }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(56.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(track.user.username, style = MaterialTheme.typography.bodySmall)
        }
        Text(formatMs(track.durationMs), style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
