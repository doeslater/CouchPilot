package com.example.couchpilot

object AppEndpoint {
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    const val TVMAZE_BASE_URL = "https://api.tvmaze.com/"
    const val WATCHMODE_BASE_URL = "https://api.watchmode.com/v1/"
    const val PLAY_STORE_BASE_URL = "https://play.google.com/store/apps/details?id="

    object Tmdb {
        // https://api.themoviedb.org/3/trending/tv/{time_window}
        const val TRENDING_TV = "trending/tv/{time_window}"

        // https://api.themoviedb.org/3/watch/providers/tv
        const val WATCH_PROVIDERS_TV = "watch/providers/tv"

        // https://api.themoviedb.org/3/discover/tv
        const val DISCOVER_TV = "discover/tv"

        // https://api.themoviedb.org/3/find/{external_id}
        const val FIND_EXTERNAL = "find/{external_id}"

        // https://api.themoviedb.org/3/tv/{series_id}/watch/providers
        const val TV_SHOW_WATCH_PROVIDERS = "tv/{series_id}/watch/providers"

        // https://api.themoviedb.org/3/tv/{series_id}
        const val TV_SHOW_DETAILS = "tv/{series_id}"

        // https://api.themoviedb.org/3/tv/top_rated
        const val TOP_RATED_TV = "tv/top_rated"

        // https://api.themoviedb.org/3/tv/popular
        const val POPULAR_TV = "tv/popular"
    }

    object TvMaze {
        // https://api.tvmaze.com/schedule
        const val SCHEDULE = "schedule"

        // https://api.tvmaze.com/lookup/shows
        const val SHOW_LOOKUP = "lookup/shows"

        // https://api.tvmaze.com/search/shows
        const val SHOW_SEARCH = "search/shows"

        // https://api.tvmaze.com/shows/{id}
        const val SHOW_DETAILS = "shows/{id}"
    }

    object Watchmode {
        // https://api.watchmode.com/v1/title/{title_id}/sources/
        const val TITLE_SOURCES = "title/{title_id}/sources/"
        // https://api.watchmode.com/v1/search/
        const val SEARCH = "search/"
    }

    object WebSearch {
        const val BBC_IPLAYER = "https://www.bbc.co.uk/iplayer/search?q="
        const val ITVX = "https://www.itv.com/watch/search?q="
        const val CHANNEL_4 = "https://www.google.com/search?q=site%3Achannel4.com+"
        const val CHANNEL_5 = "https://www.channel5.com/search?q="
        const val NETFLIX = "https://www.netflix.com/search?q="
        const val DISNEY_PLUS = "https://www.disneyplus.com/search?q="
        const val AMAZON_PRIME = "https://www.primevideo.com/search/?phrase="
        const val NOW = "https://www.nowtv.com/search?q="
        const val SKY_GO = "https://www.sky.com/watch/search?q="
        const val UKTV = "https://u.co.uk/search?q="

        // Fallback for a search result with no UK streaming sources - a name-based IMDb search
        // rather than an exact page, since we may not have (or may not trust) an id for the title.
        const val IMDB = "https://www.imdb.com/find/?q="
    }
}
