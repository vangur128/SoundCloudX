package com.example.soundcloudx.sc

import android.content.Context
import android.content.SharedPreferences
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
    
    private const val PREFS_NAME = "MeloPrefs"
    private const val KEY_CLIENT_ID = "soundcloud_client_id"
    
    fun getSavedClientId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLIENT_ID, "") ?: ""
    }
    
    fun saveClientId(context: Context, clientId: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLIENT_ID, clientId).apply()
        Log.d("MeloSC", "Ключ сохранён: ${clientId.take(10)}...")
    }

    suspend fun search(query: String, limit: Int = 50, context: Context? = null): List<SCTrack> = withContext(Dispatchers.IO) {
        val clientId = context?.let { getSavedClientId(it) } ?: ""
        if (clientId.isBlank()) throw Exception("Введи client_id в настройках")
        
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&limit=$limit&client_id=$clientId"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Пустой ответ")
        
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ключ не работает, обнови в настройках")
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
    
    suspend fun getStreamUrl(track: SCTrack, context: Context? = null): String = withContext(Dispatchers.IO) {
        if (track.streamUrl.isBlank()) throw Exception("Стрим недоступен")
        
        val clientId = context?.let { getSavedClientId(it) } ?: ""
        if (clientId.isBlank()) throw Exception("Введи client_id в настройках")
        
        val url = "${track.streamUrl}?client_id=$clientId"
        
        val response = client.newCall(Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()).execute()
        
        val body = response.body?.string() ?: throw Exception("Пустой ответ стрима")
        val json = json.parseToJsonElement(body).jsonObject
        json["url"]?.jsonPrimitive?.contentOrNull ?: throw Exception("URL стрима не найден")
    }
}
