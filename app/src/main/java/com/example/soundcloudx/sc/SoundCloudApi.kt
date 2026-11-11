package com.example.soundcloudx.sc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

object SoundCloudApi {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var clientId: String? = null

    private suspend fun getClientId(): String = withContext(Dispatchers.IO) {
        clientId?.let { return@withContext it }
        val request = Request.Builder().url("https://soundcloud.com").build()
        val html = http.newCall(request).execute().body?.string() ?: throw Exception("Нет ответа")
        
        val scriptUrls = Regex("""src="(https://[^"]*sndcdn\.com/assets/[^"]+\.js)"""")
            .findAll(html).map { it.groupValues[1] }.toList().reversed()
            
        for (u in scriptUrls.take(6)) {
            val js = http.newCall(Request.Builder().url(u).build()).execute().body?.string() ?: continue
            Regex("""client_id\s*:\s*"([A-Za-z0-9]{20,})"""").find(js)?.let {
                clientId = it.groupValues[1]
                return@withContext clientId!!
            }
        }
        throw Exception("Не удалось найти client_id")
    }

    suspend fun search(query: String): List<SCTrack> = withContext(Dispatchers.IO) {
        val cid = getClientId()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&limit=20&client_id=$cid"
        
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        val body = response.body?.string() ?: throw Exception("Пустой ответ")
        
        json.decodeFromString<SCSearchResponse>(body).collection
    }

    suspend fun getStreamUrl(track: SCTrack): String = withContext(Dispatchers.IO) {
        val cid = getClientId()
        val transcoding = track.media.transcodings.firstOrNull { 
            it.format.protocol == "progressive" 
        } ?: track.media.transcodings.first()
        
        val url = "${transcoding.url}?client_id=$cid"
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        val body = response.body?.string() ?: throw Exception("Пустой ответ стрима")
        
        json.decodeFromString<SCStreamResponse>(body).url
    }
}
