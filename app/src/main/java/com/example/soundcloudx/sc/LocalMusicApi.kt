package com.example.soundcloudx.sc

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalMusicApi {
    suspend fun getLocalTracks(context: Context): List<SCTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SCTrack>()
        
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            
            val selection = "${MediaStore.Audio.Media.DURATION} > 5000" // Только треки длиннее 5 секунд
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )
            
            cursor?.use {
                Log.d("MeloLocal", "Найдено треков: ${it.count}")
                while (it.moveToNext()) {
                    try {
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
                        val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown"
                        val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                        
                        val uri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
                            id.toString()
                        ).toString()
                        
                        tracks.add(
                            SCTrack(
                                id = id,
                                title = title,
                                user = SCUser(0, artist, ""),
                                artworkUrl = "",
                                duration = duration,
                                streamUrl = uri,
                                isLocal = true
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("MeloLocal", "Ошибка при чтении трека", e)
                    }
                }
            }
            Log.d("MeloLocal", "Загружено локальных треков: ${tracks.size}")
        } catch (e: Exception) {
            Log.e("MeloLocal", "Ошибка загрузки локальных треков", e)
        }
        
        tracks
    }
}
