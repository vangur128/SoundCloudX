package com.example.soundcloudx.sc

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Рабочий client_id (обновлён)
    private const val CLIENT_ID = "a3e059563d7fd3372b49b37f00a00bcf"

    suspend fun search(query: String, limit: Int = 50): List<SCTrack> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&limit=$limit&client_id=$CLIENT_ID"
        
        Log.d("MeloSC", "Поиск: $query")
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Пустой ответ")
        
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${responseBody.take(100)}")
        }
        
        val root = json.parseToJsonElement(responseBody).jsonObject
        val collection = root["collection"]?.jsonArray ?: return@withContext emptyList()
        
        collection.map { element ->
            val track = element.jsonObject
            val user = track["user"]?.jsonObject
            
            val media = track["media"]?.jsonObject
            val transcodings = media?.get("transcodings")?.jsonArray
            val streamUrl = transcodings?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""
            
            SCTrack(
                id = track["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                title = track["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
                user = SCUser(
                    id = user?.get("id")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                    username = user?.get("username")?.jsonPrimitive?.contentOrNull ?: "Unknown",
                    avatarUrl = ""
                ),
                artworkUrl = track["artwork_url"]?.jsonPrimitive?.contentOrNull?.replace("large", "t500x500") ?: "",
                duration = track["duration"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                streamUrl = streamUrl,
                playCount = 0,
                likesCount = 0
            )
        }
    }
    
    suspend fun getStreamUrl(track: SCTrack): String = withContext(Dispatchers.IO) {
        if (track.streamUrl.isBlank()) throw Exception("Стрим недоступен")
        
        val url = "${track.streamUrl}?client_id=$CLIENT_ID"
        
        val response = client.newCall(Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()).execute()
        
        val body = response.body?.string() ?: throw Exception("Пустой ответ стрима")
        val json = json.parseToJsonElement(body).jsonObject
        json["url"]?.jsonPrimitive?.contentOrNull ?: throw Exception("URL стрима не найден")
    }
}
