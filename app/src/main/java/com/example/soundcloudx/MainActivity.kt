package com.example.soundcloudx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import com.example.soundcloudx.ui.PlayerScreen
import com.example.soundcloudx.ui.SearchScreen

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerViewModel.initPlayer(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var showPlayer by remember { mutableStateOf(false) }

                if (showPlayer && playerViewModel.currentTrack != null) {
                    PlayerScreen(
                        playerViewModel = playerViewModel,
                        context = this,
                        onBackClick = { showPlayer = false }
                    )
                } else {
                    SearchScreen(
                        playerViewModel = playerViewModel,
                        context = this,
                        onTrackClick = { showPlayer = true } 
                    )
                }
            }
        }
    }
}
