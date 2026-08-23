package com.example.couchpilot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.couchpilot.bookmarks.data.local.BookmarkDao
import com.example.couchpilot.bookmarks.data.local.BookmarkEntity
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
        SwipeEventEntity::class,
        BookmarkEntity::class
    ],
    version = 7, // v7: added BookmarkEntity (saved-for-later shows)
    exportSchema = false
)
abstract class CouchPilotDatabase : RoomDatabase() {
    abstract val tvShowDao: TvShowDao
    abstract val scheduleDao: ScheduleDao
    abstract val swipeEventDao: SwipeEventDao
    abstract val bookmarkDao: BookmarkDao

    companion object {
        const val DATABASE_NAME = "couchpilot.db"
    }
}
