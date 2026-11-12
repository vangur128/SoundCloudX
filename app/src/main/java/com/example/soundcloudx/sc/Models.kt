package com.example.soundcloudx.sc

import kotlinx.serialization.Serializable

@Serializable
data class SCTrack(
    val id: Long,
    val title: String,
    val user: SCUser,
    val artworkUrl: String,
    val duration: Long,
    val streamUrl: String,
    val downloadUrl: String? = null,
    val playCount: Long = 0,
    val likesCount: Long = 0,
    val isLocal: Boolean = false
)

@Serializable
data class SCUser(
    val id: Long,
    val username: String,
    val avatarUrl: String
)
