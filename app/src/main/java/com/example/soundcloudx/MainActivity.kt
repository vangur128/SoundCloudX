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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.soundcloudx.db.*
import com.example.soundcloudx.sc.LocalMusicApi
import com.example.soundcloudx.sc.SCUser // <-- ДОБАВИЛИ ЭТОТ ИМПОРТ
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
                val streamUrl = SoundCloudApi.getStreamUrl(track, context)
                exoPlayer?.setMediaItem(MediaItem.fromUri(streamUrl))
                exoPlayer?.prepare()
                exoPlayer?.play()
                isPlaying = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun playLocalTrack(track: SCTrack, context: Context, trackList: List<SCTrack> = emptyList()) {
        currentTrack = track
        if (trackList.isNotEmpty()) { queue = trackList; queueIndex = trackList.indexOf(track).coerceAtLeast(0) }
        initPlayer(context)
        exoPlayer?.setMediaItem(MediaItem.fromUri(track.streamUrl))
        exoPlayer?.prepare()
        exoPlayer?.play()
        isPlaying = true
    }

    fun togglePlayPause() { if (isPlaying) { exoPlayer?.pause(); isPlaying = false } else { exoPlayer?.play(); isPlaying = true } }
    fun seekTo(position: Long) { exoPlayer?.seekTo(position); currentPosition = position }
    fun nextTrack(context: Context) {
        if (queue.isEmpty()) return
        queueIndex = if (isShuffle) (0 until queue.size).random() else (queueIndex + 1) % queue.size
        val next = queue[queueIndex]
        if (next.isLocal) playLocalTrack(next, context, queue) else playTrack(next, context, queue)
    }
    fun previousTrack(context: Context) {
        if (queue.isEmpty()) return
        if (currentPosition > 3000) { seekTo(0); return }
        queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
        val prev = queue[queueIndex]
        if (prev.isLocal) playLocalTrack(prev, context, queue) else playTrack(prev, context, queue)
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
                
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var clientIdInput by remember { mutableStateOf(SoundCloudApi.getSavedClientId(context)) }
    var mediaLibrarySection by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val db = remember { MeloDatabase.getDatabase(context) }
    
    val recentTracks by db.trackDao().getRecentTracks().collectAsState(initial = emptyList())
    val favoriteTracks by db.trackDao().getFavoriteTracks().collectAsState(initial = emptyList())
    val playlists by db.playlistDao().getAllPlaylists().collectAsState(initial = emptyList())
    
    var localTracks by remember { mutableStateOf<List<SCTrack>>(emptyList()) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            localTracks = LocalMusicApi.getLocalTracks(context)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1025)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Melo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9A8E8))
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, "Настройки", tint = Color(0xFFC9A8E8))
            }
        }
        
        TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF2A1F35), contentColor = Color(0xFFC9A8E8)) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; mediaLibrarySection = null }, text = { Text("Поиск", fontSize = 14.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; mediaLibrarySection = null }, text = { Text("Медиатека", fontSize = 14.sp) })
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
                        val found = SoundCloudApi.search(query, limit = 30, context)
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
            Spacer(Modifier.height(16.dp))
            errorMessage?.let { error ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF331133))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFFF6B9B))
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = Color(0xFFFF6B9B), modifier = Modifier.weight(1f))
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { track ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { 
                        playerViewModel.playTrack(track, context, results)
                        scope.launch {
                            db.trackDao().insertTrack(track.toEntity())
                            db.historyDao().addToHistory(HistoryEntity(trackId = track.id))
                        }
                        onTrackClick() 
                    }
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
            }
        } else if (mediaLibrarySection == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { MediaLibraryItem(icon = Icons.Default.MusicNote, title = "Локальная музыка", count = localTracks.size, onClick = { mediaLibrarySection = "local" }) }
                item { MediaLibraryItem(icon = Icons.Default.Favorite, title = "Избранное", count = favoriteTracks.size, onClick = { mediaLibrarySection = "favorites" }) }
                item { MediaLibraryItem(icon = Icons.Default.History, title = "Недавно прослушано", count = recentTracks.size, onClick = { mediaLibrarySection = "history" }) }
                item { MediaLibraryItem(icon = Icons.Default.QueueMusic, title = "Плейлисты", count = playlists.size, onClick = { }) }
                item { MediaLibraryItem(icon = Icons.Default.Settings, title = "Настройки SoundCloud", count = null, onClick = { showSettings = true }) }
            }
        } else if (mediaLibrarySection == "local") {
            BackButton { mediaLibrarySection = null }
            if (localTracks.isEmpty()) Text("Локальные треки не найдены.", color = Color.Gray, modifier = Modifier.padding(16.dp))
            else TrackList(tracks = localTracks.map { it.toEntity() }, isLocal = true, onPlay = { entity ->
                val sc = entity.toSCTrack()
                playerViewModel.playLocalTrack(sc, context, localTracks)
                scope.launch { db.historyDao().addToHistory(HistoryEntity(trackId = entity.id)) }
                onTrackClick()
            }, db = db, scope = scope)
        } else if (mediaLibrarySection == "favorites") {
            BackButton { mediaLibrarySection = null }
            if (favoriteTracks.isEmpty()) Text("Избранное пусто.", color = Color.Gray, modifier = Modifier.padding(16.dp))
            else TrackList(tracks = favoriteTracks, isLocal = false, onPlay = { entity ->
                val sc = entity.toSCTrack()
                playerViewModel.playTrack(sc, context, favoriteTracks.map { it.toSCTrack() })
                scope.launch { db.historyDao().addToHistory(HistoryEntity(trackId = entity.id)) }
                onTrackClick()
            }, db = db, scope = scope, showRemoveFavorite = true)
        } else if (mediaLibrarySection == "history") {
            BackButton { mediaLibrarySection = null }
            if (recentTracks.isEmpty()) Text("История пуста. Послушай что-нибудь!", color = Color.Gray, modifier = Modifier.padding(16.dp))
            else TrackList(tracks = recentTracks, isLocal = false, onPlay = { entity ->
                val sc = entity.toSCTrack()
                if (sc.isLocal) playerViewModel.playLocalTrack(sc, context) else playerViewModel.playTrack(sc, context)
                scope.launch { db.historyDao().addToHistory(HistoryEntity(trackId = entity.id)) }
                onTrackClick()
            }, db = db, scope = scope)
        }
    }
    
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Настройки SoundCloud", color = Color.White) },
            text = {
                Column {
                    Text("Вставь client_id:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = clientIdInput, onValueChange = { clientIdInput = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9B7CB6), unfocusedBorderColor = Color(0xFF3D2E4A)))
                }
            },
            confirmButton = { Button(onClick = { SoundCloudApi.saveClientId(context, clientIdInput); showSettings = false }) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Отмена") } },
            containerColor = Color(0xFF2A1F35)
        )
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

@Composable
fun BackButton(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFC9A8E8))
        Spacer(Modifier.width(8.dp))
        Text("Назад", color = Color(0xFFC9A8E8), fontSize = 16.sp)
    }
}

@Composable
fun TrackList(
    tracks: List<TrackEntity>, 
    isLocal: Boolean, 
    onPlay: (TrackEntity) -> Unit, 
    db: MeloDatabase, 
    scope: kotlinx.coroutines.CoroutineScope,
    showRemoveFavorite: Boolean = false
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tracks) { track ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onPlay(track) }
                .background(Color(0xFF2A1F35), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (track.artworkUrl.isNotBlank()) AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF3D2E4A)))
                else Icon(Icons.Default.MusicNote, null, tint = Color(0xFF9B7CB6), modifier = Modifier.size(48.dp).background(Color(0xFF3D2E4A), RoundedCornerShape(4.dp)).padding(8.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = Color(0xFF9B7CB6), fontSize = 12.sp, maxLines = 1)
                }
                if (showRemoveFavorite) {
                    IconButton(onClick = { scope.launch { db.favoriteDao().removeFavorite(track.id) } }) {
                        Icon(Icons.Default.Favorite, "Remove", tint = Color(0xFFFF6B9B), modifier = Modifier.size(24.dp))
                    }
                } else {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color(0xFFC9A8E8), modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

// --- PLAYER SCREEN ---
@Composable
fun PlayerScreenView(playerViewModel: PlayerViewModel, context: Context, onBackClick: () -> Unit) {
    val track = playerViewModel.currentTrack ?: return
    val isPlaying = playerViewModel.isPlaying
    val db = MeloDatabase.getDatabase(context)
    var isFavorite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(isPlaying) { while (isPlaying) { playerViewModel.updatePosition(); delay(1000) } }
    LaunchedEffect(track.id) {
        isFavorite = db.favoriteDao().isFavorite(track.id)
    }

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1025), Color(0xFF0D0815)))).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFFC9A8E8)) }
            Text("Сейчас играет", color = Color(0xFFC9A8E8), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = { }) { Icon(Icons.Default.Share, "Share", tint = Color(0xFFC9A8E8)) }
        }
        Spacer(Modifier.height(32.dp))
        AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(320.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF3D2E4A)).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(track.user.username, color = Color(0xFF9B7CB6), fontSize = 16.sp, maxLines = 1)
            }
            IconButton(onClick = { 
                scope.launch {
                    if (isFavorite) {
                        db.favoriteDao().removeFavorite(track.id)
                        isFavorite = false
                    } else {
                        db.trackDao().insertTrack(track.toEntity())
                        db.favoriteDao().addFavorite(FavoriteEntity(trackId = track.id))
                        isFavorite = true
                    }
                }
            }) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", 
                    tint = if (isFavorite) Color(0xFFFF6B9B) else Color(0xFFC9A8E8), modifier = Modifier.size(32.dp))
            }
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

@Composable
fun MediaLibraryItem(icon: ImageVector, title: String, count: Int?, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(Color(0xFF2A1F35), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFFC9A8E8), modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        if (count != null) Text("$count", color = Color(0xFF9B7CB6), fontSize = 14.sp)
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9B7CB6))
    }
}

fun SCTrack.toEntity() = TrackEntity(id = id, title = title, artist = user.username, artworkUrl = artworkUrl, duration = duration, streamUrl = streamUrl, isLocal = isLocal)
fun TrackEntity.toSCTrack() = SCTrack(id = id, title = title, user = SCUser(0, artist, ""), artworkUrl = artworkUrl, duration = duration, streamUrl = streamUrl, isLocal = isLocal)

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
