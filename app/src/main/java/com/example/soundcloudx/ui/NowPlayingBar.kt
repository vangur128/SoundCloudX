package com.example.soundcloudx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.soundcloudx.PlayerViewModel
import com.example.soundcloudx.sc.SCTrack

@Composable
fun NowPlayingBar(
    track: SCTrack,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF282828))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Сейчас играет",
                color = Color(0xFF1DB954),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.user.username,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = { playerViewModel.togglePlayPause() }) {
            Icon(
                imageVector = if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Toggle Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
