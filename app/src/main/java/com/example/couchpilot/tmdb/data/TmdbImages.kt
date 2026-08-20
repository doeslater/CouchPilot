package com.example.couchpilot.tmdb.data

object TmdbImages {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    fun posterUrl(path: String?, size: String = "w500"): String? = imageUrl(path, size)

    fun logoUrl(path: String?, size: String = "original"): String? = imageUrl(path, size)

    private fun imageUrl(path: String?, size: String): String? = path?.let { "$BASE_URL$size$it" }
}
