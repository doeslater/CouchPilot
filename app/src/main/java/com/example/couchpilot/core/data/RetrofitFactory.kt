package com.example.couchpilot.core.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Shared Retrofit builder so every feature's `provide<X>Service` Hilt method (TMDB, TVmaze, ...)
 * doesn't hand-roll its own identical OkHttpClient+GsonConverterFactory pipeline - only the base
 * URL differs between them.
 */
object RetrofitFactory {
    fun create(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
