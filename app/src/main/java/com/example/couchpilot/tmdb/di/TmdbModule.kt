package com.example.couchpilot.tmdb.di

import com.example.couchpilot.tmdb.data.DefaultTmdbRepository
import com.example.couchpilot.tmdb.data.TmdbService
import com.example.couchpilot.tmdb.domain.TmdbRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbModule {

    @Binds
    @Singleton
    abstract fun bindTmdbRepository(impl: DefaultTmdbRepository): TmdbRepository

    companion object {
        @Provides
        @Singleton
        fun provideTmdbService(retrofit: Retrofit): TmdbService {
            return retrofit.create(TmdbService::class.java)
        }
    }
}
