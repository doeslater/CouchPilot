package com.example.couchpilot.tmdb.data

import com.example.couchpilot.AppEndpoint

object TmdbImages {
    private const val BASE_URL = AppEndpoint.TMDB_IMAGE_BASE_URL

    fun posterUrl(path: String?, size: String = "w500"): String? = imageUrl(path, size)

    fun logoUrl(path: String?, size: String = "original"): String? = imageUrl(path, size)

    private fun imageUrl(path: String?, size: String): String? = path?.let { "$BASE_URL$size$it" }
}
