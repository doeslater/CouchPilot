package com.example.couchpilot.watchmode.domain

data class WatchmodeSearchResult(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    /** True for a TV show/series result, false for a movie (or any other Watchmode result_type). */
    val isTvShow: Boolean,
    /**
     * TMDB id for this title, when Watchmode was able to resolve one. Lets a search hit be routed
     * into ShowDetailScreen (and therefore the taste-scoring/vote/dwell-time signals) the same way
     * a Tonight/Discover result is, instead of only ever reaching the granular Watchmode streaming
     * sources screen. Null means Watchmode couldn't map this title to TMDB.
     */
    val tmdbId: Int?
)
