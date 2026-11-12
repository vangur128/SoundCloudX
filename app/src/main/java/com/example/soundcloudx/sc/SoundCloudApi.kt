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
    
    private var cachedClientId: String? = null
    private var isFetchingClientId = false

    private suspend fun getClientId(): String = withContext(Dispatchers.IO) {
        cachedClientId?.let { return@withContext it }
        
        if (isFetchingClientId) {
            while (cachedClientId == null) {
                kotlinx.coroutines.delay(500)
            }
            return@withContext cachedClientId!!
        }
        
        isFetchingClientId = true
        Log.d("MeloSC", "Добыча client_id...")
        
        try {
            val mainPage = client.newCall(Request.Builder()
                .url("https://soundcloud.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()).execute().body?.string() ?: throw Exception("Не удалось загрузить SoundCloud")
            
            val jsPattern = Regex("""https://a-v2\.sndcdn\.com/assets/[0-9a-f]+-[^"]+\.js""")
            val jsUrl = jsPattern.find(mainPage)?.value ?: throw Exception("JS-файл не найден")
            
            Log.d("MeloSC", "JS: $jsUrl")
            
            val jsContent = client.newCall(Request.Builder()
                .url(jsUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()).execute().body?.string() ?: throw Exception("JS не загружен")
            
            val clientIdPatterns = listOf(
                Regex("""client_id:"([^"]+)"""")
            )
            
            val clientId = clientIdPatterns.firstNotNullOfOrNull { pattern ->
                pattern.find(jsContent)?.groupValues?.get(1)
            } ?: throw Exception("client_id не найден в JS")
            
            cachedClientId = clientId
            Log.d("MeloSC", "client_id получен: ${clientId.take(10)}...")
            clientId
        } finally {
            isFetchingClientId = false
        }
    }

    suspend fun search(query: String, limit: Int = 50): List<SCTrack> = withContext(Dispatchers.IO) {
        val clientId = getClientId()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&limit=$limit&client_id=$clientId"
        
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
        
        val clientId = getClientId()
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
