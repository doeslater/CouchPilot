package com.example.couchpilot.tmdb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowDao {
    @Query("SELECT * FROM tv_shows")
    fun getAllTvShows(): Flow<List<TvShowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvShows(shows: List<TvShowEntity>)

    @Query("DELETE FROM tv_shows")
    suspend fun clearAll()

    @Query("SELECT * FROM tv_shows WHERE id = :id")
    suspend fun getTvShowById(id: Int): TvShowEntity?
}
