package com.example.couchpilot.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.couchpilot.core.database.CouchPilotDatabase
import com.example.couchpilot.tmdb.data.local.TvShowDao
import com.example.couchpilot.tvmaze.data.local.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CouchPilotDatabase {
        return Room.databaseBuilder(
            context,
            CouchPilotDatabase::class.java,
            CouchPilotDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideTvShowDao(db: CouchPilotDatabase): TvShowDao {
        return db.tvShowDao
    }

    @Provides
    @Singleton
    fun provideScheduleDao(db: CouchPilotDatabase): ScheduleDao {
        return db.scheduleDao
    }

    @Provides
    @Singleton
    fun provideSwipeEventDao(db: CouchPilotDatabase): com.example.couchpilot.onboarding.data.local.SwipeEventDao {
        return db.swipeEventDao
    }
}
