package com.example.couchpilot.watchmode.di

import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.core.data.RetrofitFactory
import com.example.couchpilot.watchmode.data.DefaultWatchmodeRepository
import com.example.couchpilot.watchmode.data.WatchmodeService
import com.example.couchpilot.watchmode.domain.WatchmodeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchmodeModule {

    @Binds
    @Singleton
    abstract fun bindWatchmodeRepository(impl: DefaultWatchmodeRepository): WatchmodeRepository

    companion object {
        @Provides
        @Singleton
        fun provideWatchmodeService(okHttpClient: OkHttpClient): WatchmodeService {
            return RetrofitFactory.create(okHttpClient, AppEndpoint.WATCHMODE_BASE_URL)
                .create(WatchmodeService::class.java)
        }
    }
}
