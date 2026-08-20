package com.example.couchpilot.tmdb.data

object TmdbImages {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    fun posterUrl(path: String?, size: String = "w500"): String? {
        return path?.let { "$BASE_URL$size$it" }
    }

    fun logoUrl(path: String?, size: String = "original"): String? {
        return path?.let { "$BASE_URL$size$it" }
    }
}
