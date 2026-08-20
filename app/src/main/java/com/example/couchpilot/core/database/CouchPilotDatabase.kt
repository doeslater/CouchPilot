package com.example.couchpilot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.couchpilot.tmdb.data.local.TvShowDao
import com.example.couchpilot.tmdb.data.local.TvShowEntity
import com.example.couchpilot.tvmaze.data.local.ScheduleDao
import com.example.couchpilot.tvmaze.data.local.ScheduleItemEntity

@Database(
    entities = [
        TvShowEntity::class,
        ScheduleItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CouchPilotDatabase : RoomDatabase() {
    abstract val tvShowDao: TvShowDao
    abstract val scheduleDao: ScheduleDao

    companion object {
        const val DATABASE_NAME = "couchpilot.db"
    }
}
