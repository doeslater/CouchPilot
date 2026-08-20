package com.example.couchpilot.core.di

import com.example.couchpilot.core.data.DefaultLocalDataManager
import com.example.couchpilot.core.domain.LocalDataManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalDataModule {
    @Binds
    abstract fun bindLocalDataManager(impl: DefaultLocalDataManager): LocalDataManager
}
