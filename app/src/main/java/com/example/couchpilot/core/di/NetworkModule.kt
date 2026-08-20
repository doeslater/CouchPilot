package com.example.couchpilot.core.di

import com.example.couchpilot.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides a single, plain [HttpClient] shared by every remote data source in the app (TMDB today,
 * potentially TVmaze later per general_idea.md). Deliberately has no auth wired in here — a token
 * that's specific to one API (like TMDB's) belongs on that API's own requests, not on every request
 * this shared client makes, or it would leak to whatever other API gets added next.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
        }
    }
}
