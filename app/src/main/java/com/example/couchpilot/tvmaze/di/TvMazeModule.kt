package com.example.couchpilot.tvmaze.di

import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.core.data.RetrofitFactory
import com.example.couchpilot.tvmaze.data.DefaultTvMazeRepository
import com.example.couchpilot.tvmaze.data.TvMazeService
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TvMazeModule {

    @Binds
    @Singleton
    abstract fun bindTvMazeRepository(impl: DefaultTvMazeRepository): TvMazeRepository

    companion object {
        @Provides
        @Singleton
        fun provideTvMazeService(okHttpClient: OkHttpClient): TvMazeService {
            return RetrofitFactory.create(okHttpClient, AppEndpoint.TVMAZE_BASE_URL)
                .create(TvMazeService::class.java)
        }
    }
}
