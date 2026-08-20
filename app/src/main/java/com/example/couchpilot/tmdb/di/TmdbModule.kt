package com.example.couchpilot.tmdb.di

import com.example.couchpilot.tmdb.data.DefaultTmdbRepository
import com.example.couchpilot.tmdb.domain.TmdbRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbModule {
    @Binds
    @Singleton
    abstract fun bindTmdbRepository(impl: DefaultTmdbRepository): TmdbRepository
}
