package com.example.soundcloudx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.soundcloudx.ui.*

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerViewModel.initPlayer(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != "player") {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Поиск") },
                                    selected = currentRoute == "search",
                                    onClick = { navController.navigate("search") }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Player") },
                                    label = { Text("Плеер") },
                                    selected = currentRoute == "player",
                                    onClick = { navController.navigate("player") }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "search",
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        composable("search") {
                            SearchScreen(
                                playerViewModel = playerViewModel,
                                onTrackClick = { track -> navController.navigate("player") }
                            )
                        }
                        composable("player") {
                            PlayerScreen(
                                playerViewModel = playerViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }

                playerViewModel.currentTrack?.let { track ->
                    if (currentRoute != "player") {
                        NowPlayingBar(
                            track = track,
                            playerViewModel = playerViewModel,
                            onClick = { navController.navigate("player") }
                        )
                    }
                }
            }
        }
    }
                    }
