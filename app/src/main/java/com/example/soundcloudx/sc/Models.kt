package com.example.soundcloudx.sc
import kotlinx.serialization.Serializable

@Serializable
data class SCSearchResponse(val collection: List<SCTrack>)

@Serializable
data class SCTrack(
    val id: Long,
    val title: String,
    val duration: Long,
    val artwork_url: String?,
    val permalink_url: String,
    val media: SCMedia,
    val user: SCUser
) {
    val durationMs get() = duration
    val artworkUrl get() = artwork_url?.replace("-large", "-t500x500") ?: "https://via.placeholder.com/150"
}

@Serializable
data class SCMedia(val transcodings: List<SCTranscoding>)

@Serializable
data class SCTranscoding(val url: String, val format: SCFormat)

@Serializable
data class SCFormat(val protocol: String, val mime_type: String)

@Serializable
data class SCUser(val username: String)

@Serializable
data class SCStreamResponse(val url: String)
