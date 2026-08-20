package com.example.couchpilot.tmdb.data

/** TMDB endpoint URLs. Not a secret — only the auth token (BuildConfig.TMDB_READ_ACCESS_TOKEN) is. */
object TmdbRoutes {
    private const val BASE_URL = "https://api.themoviedb.org/3"

    /** https://developer.themoviedb.org/reference/trending-tv */
    fun trendingTv(timeWindow: String = "day") = "$BASE_URL/trending/tv/$timeWindow"
}

/** https://developer.themoviedb.org/docs/image-basics — w500 is a reasonable default poster size. */
object TmdbImages {
    private const val POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"

    fun posterUrl(posterPath: String?): String? = posterPath?.let { "$POSTER_BASE_URL$it" }
}
