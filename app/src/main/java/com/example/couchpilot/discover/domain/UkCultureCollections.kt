package com.example.couchpilot.discover.domain

/**
 * UK culture collections for Discover, defined dynamically via TMDB's `/discover/tv` - either
 * `with_origin_country=GB` + `with_genres` (see [com.example.couchpilot.tmdb.domain.
 * TmdbRepository.discoverByGenre]) or `with_networks` for a specific UK broadcaster (see
 * [com.example.couchpilot.tmdb.domain.TmdbRepository.discoverByNetwork]), both with a
 * `vote_count.gte` floor so a handful of votes can't win purely on a tiny, unreliable sample.
 * Exactly one of [genreId]/[networkId] is set per entry. Unlike a hand-maintained show-id list
 * (the original approach here, and still the pattern `tvmaze/domain/FreeviewChannels.kt` uses for
 * something TMDB has no equivalent filter for), this self-refreshes as TMDB's own vote data
 * changes - no one has to remember to update it as shows fall in or out of favor.
 *
 * Genre ids are TMDB's standard TV genre ids (see `recommendation/domain/TmdbTvGenres.kt` for the
 * full id->name reference). Network ids are TMDB's internal network ids - confirmed by checking a
 * known show's `networks` field via `GET /tv/{id}` (e.g. Broadchurch -> ITV1 = 9, Peep Show ->
 * Channel 4 = 26), not guessed. "Best of BBC" was considered and skipped: BBC One/Two's top-rated
 * GB shows are already fully covered by the genre collections below, so a dedicated row would be
 * near-100% redundant. Some overlap between collections is otherwise expected and fine (e.g. a
 * show tagged both Comedy and Drama, or aired on a network that also produces prestige dramas,
 * can appear in more than one row) - real streaming-app UX has the same overlap.
 */
data class UkCultureCollection(
    val title: String,
    val minVoteCount: Int,
    val genreId: Int? = null,
    val networkId: Int? = null,
)

val UK_CULTURE_COLLECTIONS: List<UkCultureCollection> = listOf(
    UkCultureCollection(title = "Bingeable Box Sets", genreId = 80, minVoteCount = 200), // Crime
    UkCultureCollection(title = "Award-Winning British Dramas", genreId = 18, minVoteCount = 300), // Drama
    UkCultureCollection(title = "Panel Shows & Comedy", genreId = 35, minVoteCount = 200), // Comedy
    UkCultureCollection(title = "Best of British Docs", genreId = 99, minVoteCount = 100), // Documentary
    UkCultureCollection(title = "British Sci-Fi & Fantasy", genreId = 10765, minVoteCount = 150), // Sci-Fi & Fantasy
    UkCultureCollection(title = "Reality & Factual Favourites", genreId = 10764, minVoteCount = 100), // Reality
    UkCultureCollection(title = "Classic British Animation", genreId = 16, minVoteCount = 100), // Animation
    UkCultureCollection(title = "Cozy British Mysteries", genreId = 9648, minVoteCount = 150), // Mystery
    UkCultureCollection(title = "British Action & Adventure", genreId = 10759, minVoteCount = 150), // Action & Adventure
    UkCultureCollection(title = "War & Politics", genreId = 10768, minVoteCount = 50), // War & Politics
    UkCultureCollection(title = "Best of ITV", networkId = 9, minVoteCount = 100), // ITV1
    UkCultureCollection(title = "Best of Channel 4", networkId = 26, minVoteCount = 100), // Channel 4
)
