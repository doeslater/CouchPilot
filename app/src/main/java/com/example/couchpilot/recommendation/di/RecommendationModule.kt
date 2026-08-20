package com.example.couchpilot.recommendation.di

import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.recommendation.domain.RecommendationScorer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecommendationModule {

    @Provides
    @Singleton
    fun provideRecommendationScorer(swipeEventDao: SwipeEventDao): RecommendationScorer {
        return RecommendationScorer(swipeEventDao)
    }
}
