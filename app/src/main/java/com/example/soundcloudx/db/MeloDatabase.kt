package com.example.soundcloudx.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val duration: Long,
    val streamUrl: String,
    val isLocal: Boolean = false
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: Int,
    val trackId: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val trackId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_tracks")
data class HiddenTrackEntity(
    @PrimaryKey val trackId: Long,
    val hiddenAt: Long = System.currentTimeMillis()
)

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM history ORDER BY playedAt DESC LIMIT 100)")
    fun getRecentTracks(): Flow<List<TrackEntity>>
    @Query("SELECT * FROM tracks WHERE id IN (SELECT trackId FROM favorites ORDER BY addedAt DESC LIMIT 100)")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    @Query("SELECT * FROM tracks INNER JOIN playlist_tracks ON tracks.id = playlist_tracks.trackId WHERE playlist_tracks.playlistId = :playlistId")
    fun getTracksInPlaylist(playlistId: Int): Flow<List<TrackEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(playlistTrack: PlaylistTrackEntity)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: Long)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: Long): Boolean
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<HistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(history: HistoryEntity)
    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Dao
interface HiddenTrackDao {
    @Query("SELECT * FROM hidden_tracks ORDER BY hiddenAt DESC")
    fun getAllHidden(): Flow<List<HiddenTrackEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideTrack(hidden: HiddenTrackEntity)
    @Query("DELETE FROM hidden_tracks WHERE trackId = :trackId")
    suspend fun unhideTrack(trackId: Long)
    @Query("SELECT trackId FROM hidden_tracks")
    suspend fun getHiddenIds(): List<Long>
}

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, FavoriteEntity::class, HistoryEntity::class, HiddenTrackEntity::class],
    version = 3
)
abstract class MeloDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun hiddenTrackDao(): HiddenTrackDao
    
    companion object {
        @Volatile
        private var INSTANCE: MeloDatabase? = null
        
        fun getDatabase(context: Context): MeloDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeloDatabase::class.java,
                    "melo_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
