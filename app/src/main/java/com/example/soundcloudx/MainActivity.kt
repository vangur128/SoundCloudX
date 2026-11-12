package com.example.soundcloudx

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.soundcloudx.sc.LocalMusicApi
import com.example.soundcloudx.sc.SoundCloudApi
import com.example.soundcloudx.sc.SCTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- VIEW MODEL ---
class PlayerViewModel : ViewModel() {
    var exoPlayer: ExoPlayer? = null
    var mediaSession: androidx.media3.session.MediaSession? = null
    var currentTrack by mutableStateOf<SCTrack?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var queue by mutableStateOf<List<SCTrack>>(emptyList())
    var queueIndex by mutableStateOf(0)
    var isShuffle by mutableStateOf(false)
    var isRepeat by mutableStateOf(false)

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
                .build()
            
            // Создаем MediaSession для интеграции с системой
            mediaSession = androidx.media3.session.MediaSession.Builder(context, exoPlayer!!).build()
            
            exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        if (isRepeat) { exoPlayer?.seekTo(0); exoPlayer?.play() } else { nextTrack(context) }
                    }
                }
            })
        }
    }

    fun playTrack(track: SCTrack, context: Context, trackList: List<SCTrack> = emptyList()) {
        viewModelScope.launch {
            try {
                currentTrack = track
                if (trackList.isNotEmpty()) { queue = trackList; queueIndex = trackList.indexOf(track).coerceAtLeast(0) }
                initPlayer(context)
                val streamUrl = SoundCloudApi.getStreamUrl(track)
                exoPlayer?.setMediaItem(MediaItem.fromUri(streamUrl))
                exoPlayer?.prepare()
                exoPlayer?.play()
                isPlaying = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun togglePlayPause() { if (isPlaying) { exoPlayer?.pause(); isPlaying = false } else { exoPlayer?.play(); isPlaying = true } }
    fun seekTo(position: Long) { exoPlayer?.seekTo(position); currentPosition = position }
    fun nextTrack(context: Context) {
        if (queue.isEmpty()) return
        queueIndex = if (isShuffle) (0 until queue.size).random() else (queueIndex + 1) % queue.size
        playTrack(queue[queueIndex], context, queue)
    }
    fun previousTrack(context: Context) {
        if (queue.isEmpty()) return
        if (currentPosition > 3000) { seekTo(0); return }
        queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
        playTrack(queue[queueIndex], context, queue)
    }
    fun updatePosition() {
        currentPosition = exoPlayer?.currentPosition ?: 0L
        duration = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L
    }
    fun toggleShuffle() { isShuffle = !isShuffle }
    fun toggleRepeat() { isRepeat = !isRepeat }

    override fun onCleared() { super.onCleared(); mediaSession?.release(); exoPlayer?.release(); exoPlayer = null }
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerViewModel.initPlayer(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var showPlayer by remember { mutableStateOf(false) }
                
                // Запрос разрешений для локальной музыки
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(permission)
                    }
                }

                if (showPlayer && playerViewModel.currentTrack != null) {
                    PlayerScreenView(playerViewModel = playerViewModel, context = this, onBackClick = { showPlayer = false })
                } else {
                    SearchScreen(playerViewModel = playerViewModel, context = this, onTrackClick = { showPlayer = true })
                }
            }
        }
    }
}

// --- SEARCH SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(playerViewModel: PlayerViewModel, context: Context, onTrackClick: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SCTrack>>(emptyList()) }
    var localTracks by remember { mutableStateOf<List<SCTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Загружаем локальные треки при старте
    LaunchedEffect(Unit) { 
        try { localTracks = LocalMusicApi.getLocalTracks(context) } catch (_: Exception) {} 
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1025)).padding(16.dp)) {
        Text("Melo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9A8E8), modifier = Modifier.padding(bottom = 16.dp))
        
        TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF2A1F35), contentColor = Color(0xFFC9A8E8)) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("SoundCloud", fontSize = 14.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Моя музыка (${localTracks.size})", fontSize = 14.sp) })
        }
        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Введите запрос...", color = Color.Gray) }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9B7CB6)) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9B7CB6), unfocusedBorderColor = Color(0xFF3D2E4A)),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (query.isBlank()) return@Button
                isLoading = true; errorMessage = null
                scope.launch {
                    try {
                        val found = SoundCloudApi.search(query, limit = 30)
                        if (found.isEmpty()) errorMessage = "Ничего не найдено."
                        results = found
                    } catch (e: Throwable) { errorMessage = "Ошибка: ${e.toString()}"; results = emptyList() }
                    finally { isLoading = false }
                }
            }, enabled = !isLoading && query.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B7CB6), contentColor = Color.White),
               modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                else Text("Найти", fontWeight = FontWeight.Bold)
            }
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

        val displayTracks = if (selectedTab == 0) results else localTracks
        if (displayTracks.isEmpty() && selectedTab == 1) Text("Локальная музыка не найдена или нет прав доступа", color = Color.Gray, modifier = Modifier.padding(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(displayTracks) { track ->
                Row(modifier = Modifier.fillMaxWidth().clickable { playerViewModel.playTrack(track, context, displayTracks); onTrackClick() }
                    .background(Color(0xFF2A1F35), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (track.artworkUrl.isNotBlank()) AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF3D2E4A)))
                    else Icon(Icons.Default.MusicNote, null, tint = Color(0xFF9B7CB6), modifier = Modifier.size(48.dp).background(Color(0xFF3D2E4A), RoundedCornerShape(4.dp)).padding(8.dp))
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
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A1F35)).clickable { onTrackClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (track.artworkUrl.isNotBlank()) AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF3D2E4A)))
            else Icon(Icons.Default.MusicNote, null, tint = Color(0xFF9B7CB6), modifier = Modifier.size(40.dp).background(Color(0xFF3D2E4A), RoundedCornerShape(4.dp)).padding(6.dp))
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

// --- PLAYER SCREEN ---
@Composable
fun PlayerScreenView(playerViewModel: PlayerViewModel, context: Context, onBackClick: () -> Unit) {
    val track = playerViewModel.currentTrack ?: return
    val isPlaying = playerViewModel.isPlaying
    LaunchedEffect(isPlaying) { while (isPlaying) { playerViewModel.updatePosition(); delay(1000) } }
    val rotation by animateFloatAsState(targetValue = if (isPlaying) 360f else 0f, animationSpec = infiniteRepeatable(animation = tween(20000)))
    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1025), Color(0xFF0D0815)))).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFFC9A8E8)) }
            Text("Сейчас играет", color = Color(0xFFC9A8E8), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = { }) { Icon(Icons.Default.Share, "Share", tint = Color(0xFFC9A8E8)) }
        }
        Spacer(Modifier.height(32.dp))
        AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(320.dp).clip(RoundedCornerShape(24.dp)).rotate(rotation).background(Color(0xFF3D2E4A)).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(track.user.username, color = Color(0xFF9B7CB6), fontSize = 16.sp, maxLines = 1)
            }
            IconButton(onClick = { }) { Icon(Icons.Default.FavoriteBorder, "Like", tint = Color(0xFFC9A8E8), modifier = Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(32.dp))
        Slider(value = playerViewModel.currentPosition.toFloat(), onValueChange = { playerViewModel.seekTo(it.toLong()) },
            valueRange = 0f..playerViewModel.duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(thumbColor = Color(0xFFC9A8E8), activeTrackColor = Color(0xFFC9A8E8), inactiveTrackColor = Color(0xFF3D2E4A)),
            modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playerViewModel.currentPosition), color = Color(0xFF9B7CB6), fontSize = 12.sp)
            Text(formatDuration(playerViewModel.duration), color = Color(0xFF9B7CB6), fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { playerViewModel.toggleShuffle() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (playerViewModel.isShuffle) Color(0xFFC9A8E8) else Color(0xFF6B5B7B), modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { playerViewModel.previousTrack(context) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { playerViewModel.togglePlayPause() }, modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF9B7CB6))) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            IconButton(onClick = { playerViewModel.nextTrack(context) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { playerViewModel.toggleRepeat() }, modifier = Modifier.size(48.dp)) {
                Icon(if (playerViewModel.isRepeat) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (playerViewModel.isRepeat) Color(0xFFC9A8E8) else Color(0xFF6B5B7B), modifier = Modifier.size(28.dp))
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
