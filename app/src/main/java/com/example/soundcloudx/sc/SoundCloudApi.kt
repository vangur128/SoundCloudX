package com.example.soundcloudx.sc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SoundCloudApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String, limit: Int = 50): List<SCTrack> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.deezer.com/search?q=$encodedQuery&limit=$limit"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Ошибка сети: ${response.code}")
        
        val body = response.body?.string() ?: throw Exception("Пустой ответ")
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"]?.jsonArray ?: return emptyList()
        
        return data.map { element ->
            val track = element.jsonObject
            val artist = track["artist"]?.jsonObject
            val album = track["album"]?.jsonObject
            
            SCTrack(
                id = track["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                title = track["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
                user = SCUser(
                    id = artist?.get("id")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                    username = artist?.get("name")?.jsonPrimitive?.contentOrNull ?: "Unknown",
                    avatarUrl = ""
                ),
                artworkUrl = album?.get("cover_big")?.jsonPrimitive?.contentOrNull 
                    ?: album?.get("cover_medium")?.jsonPrimitive?.contentOrNull 
                    ?: album?.get("cover")?.jsonPrimitive?.contentOrNull 
                    ?: "",
                duration = (track["duration"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0) * 1000,
                streamUrl = track["preview"]?.jsonPrimitive?.contentOrNull ?: "",
                playCount = 0,
                likesCount = 0
            )
        }
    }
    
    suspend fun getStreamUrl(track: SCTrack): String {
        if (track.streamUrl.isBlank()) throw Exception("Превью недоступно")
        return track.streamUrl
    }
}
