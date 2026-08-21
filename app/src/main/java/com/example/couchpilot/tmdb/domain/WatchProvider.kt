package com.example.couchpilot.tmdb.domain

data class WatchProvider(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val tmdbUrl: String? = null
)
