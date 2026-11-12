package com.example.soundcloudx.sc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object SoundCloudApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // ВСТАВЬ СЮДА СВОЙ НОВЫЙ КЛЮЧ ВМЕСТО ЭТОГО, ЕСЛИ ПОИСК ВСЁ ЕЩЁ НЕ РАБОТАЕТ
    private const val CLIENT_ID = "2t9loNQH90kzJcsFCODdigxfp325aq4z"
    
    suspend fun search(query: String, limit: Int = 50): List<SCTrack> {
        val url = "https://api-v2.soundcloud.com/search/tracks?q=$query&limit=$limit&client_id=$CLIENT_ID"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        
        val jsonObject = json.parseToJsonElement(body).jsonObject
        val collection = jsonObject["collection"]?.jsonArray ?: return emptyList()
        
        return collection.map { trackElement ->
            val track = trackElement.jsonObject
            
            val userObj = track["user"]?.jsonObject
            val user = SCUser(
                id = track["user_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                username = userObj?.get("username")?.jsonPrimitive?.contentOrNull ?: "Unknown",
                avatarUrl = userObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull ?: ""
            )
            
            val mediaObj = track["media"]?.jsonObject
            val transcodings = mediaObj?.get("transcodings")?.jsonArray
            val streamUrl = transcodings?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""
            
            SCTrack(
                id = track["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                title = track["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
                user = user,
                artworkUrl = track["artwork_url"]?.jsonPrimitive?.contentOrNull?.replace("large", "t500x500") ?: "",
                duration = track["duration"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                streamUrl = streamUrl,
                playCount = track["playback_count"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                likesCount = track["likes_count"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0
            )
        }
    }
    
    suspend fun getStreamUrl(track: SCTrack): String {
        if (track.streamUrl.isBlank()) return ""
        
        val url = "${track.streamUrl}?client_id=$CLIENT_ID"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty stream response")
        
        val jsonResponse = json.parseToJsonElement(body).jsonObject
        return jsonResponse["url"]?.jsonPrimitive?.contentOrNull ?: throw Exception("No stream URL")
    }
}
