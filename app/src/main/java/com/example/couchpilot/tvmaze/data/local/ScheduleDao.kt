package com.example.couchpilot.tvmaze.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items WHERE date = :date")
    fun getScheduleForDate(date: String): Flow<List<ScheduleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItems(items: List<ScheduleItemEntity>)

    @Query("DELETE FROM schedule_items WHERE date < :date")
    suspend fun deleteOldSchedules(date: String)

    @Query("UPDATE schedule_items SET posterUrl = :posterUrl WHERE showId = :showId")
    suspend fun updatePosterUrl(showId: Int, posterUrl: String?)
}
