package com.example.couchpilot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.couchpilot.tmdb.data.local.TvShowDao
import com.example.couchpilot.tmdb.data.local.TvShowEntity
import com.example.couchpilot.tvmaze.data.local.ScheduleDao
import com.example.couchpilot.tvmaze.data.local.ScheduleItemEntity
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity

@Database(
    entities = [
        TvShowEntity::class,
        ScheduleItemEntity::class,
        SwipeEventEntity::class
    ],
    version = 3, // v3: added TvShowEntity.genreIds
    exportSchema = false
)
abstract class CouchPilotDatabase : RoomDatabase() {
    abstract val tvShowDao: TvShowDao
    abstract val scheduleDao: ScheduleDao
    abstract val swipeEventDao: SwipeEventDao

    companion object {
        const val DATABASE_NAME = "couchpilot.db"
    }
}
