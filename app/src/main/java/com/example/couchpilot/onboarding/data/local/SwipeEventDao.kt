package com.example.couchpilot.onboarding.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SwipeEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwipeEvent(event: SwipeEventEntity)

    @Query("SELECT * FROM swipe_events ORDER BY timestamp DESC")
    fun getAllSwipeEvents(): Flow<List<SwipeEventEntity>>

    @Query("DELETE FROM swipe_events")
    suspend fun clearAll()
}
