package com.example.couchpilot.recommendation.domain

/**
 * Hand-maintained TMDB TV genre id -> display name map, from TMDB's own `GET /genre/tv/list` -
 * a small, fixed, rarely-changing reference list, so hardcoding it here isn't worth a network
 * call + local cache. Same "hand-maintained reference list" pattern as
 * `tvmaze/domain/FreeviewChannels.kt`. Used to make `PreferenceVector`'s genre-id keys readable
 * on `ProfileScreen` - falls back to a numeric label for any id TMDB adds later that isn't in
 * this list yet.
 */
object TmdbTvGenres {
    private val names = mapOf(
        10759 to "Action & Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        10762 to "Kids",
        9648 to "Mystery",
        10763 to "News",
        10764 to "Reality",
        10765 to "Sci-Fi & Fantasy",
        10766 to "Soap",
        10767 to "Talk",
        10768 to "War & Politics",
        37 to "Western",
    )

    fun nameFor(genreId: Int): String = names[genreId] ?: "Genre #$genreId"
}
