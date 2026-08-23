package com.example.couchpilot.discover.domain

/**
 * UK culture collections for Discover, defined dynamically via TMDB's `/discover/tv` with
 * `with_origin_country=GB` + `with_genres` + a `vote_count.gte` floor (so a genre with only a
 * handful of votes can't win purely on a tiny, unreliable sample) - see
 * [com.example.couchpilot.tmdb.domain.TmdbRepository.discoverByGenre]. Unlike a hand-maintained
 * show-id list (the previous approach here, and still the pattern `tvmaze/domain/
 * FreeviewChannels.kt` uses for something TMDB has no equivalent filter for), this self-refreshes
 * as TMDB's own vote data changes - no one has to remember to update it as shows fall in or out
 * of favor.
 *
 * Genre ids are TMDB's standard TV genre ids (see `recommendation/domain/TmdbTvGenres.kt` for the
 * full id->name reference). Some overlap between collections is expected and fine (e.g. a show
 * tagged both Comedy and Drama can appear in both rows) - real streaming-app UX has the same
 * overlap.
 */
data class UkCultureCollection(val title: String, val genreId: Int, val minVoteCount: Int)

val UK_CULTURE_COLLECTIONS: List<UkCultureCollection> = listOf(
    UkCultureCollection(title = "Bingeable Box Sets", genreId = 80, minVoteCount = 200), // Crime
    UkCultureCollection(title = "Award-Winning British Dramas", genreId = 18, minVoteCount = 300), // Drama
    UkCultureCollection(title = "Panel Shows & Comedy", genreId = 35, minVoteCount = 200), // Comedy
    UkCultureCollection(title = "Best of British Docs", genreId = 99, minVoteCount = 100), // Documentary
)
