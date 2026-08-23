package com.example.couchpilot.tmdb.domain

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result

/**
 * TMDB is the only data source behind this today (see [com.example.couchpilot.tmdb.data.RetrofitTmdbRemoteDataSource]),
 * but it's named — and typed on the broader [DataError], not [DataError.Network] — as a repository rather than a
 * data source because it's the seam where a local Room cache gets added later (GENERAL_IDEA.md's offline-first
 * "Smart Cache" idea) without presentation code needing to change.
 */
interface TmdbRepository {
    suspend fun getTrendingTvShows(providerId: Int? = null): Result<List<TvShow>, DataError>
    suspend fun getWatchProviders(): Result<List<WatchProvider>, DataError>
    suspend fun getTvShowByImdbId(imdbId: String): Result<TvShow?, DataError>
    suspend fun getTvShowById(id: Int): Result<TvShow?, DataError>
    suspend fun getWatchProvidersForShow(tvId: Int): Result<List<WatchProvider>, DataError>
    suspend fun search(query: String): Result<List<TvShow>, DataError>

    /** GB-origin shows in [genreId], sorted by vote_average desc, filtered to at least
     *  [minVoteCount] votes so obscure/low-signal entries don't win purely on a small sample -
     *  powers Discover's UK Culture Collections (`discover/domain/UkCultureCollections.kt`). */
    suspend fun discoverByGenre(genreId: Int, minVoteCount: Int): Result<List<TvShow>, DataError>

    /** Shows aired on TMDB network [networkId] (e.g. ITV1, Channel 4 - real TMDB network ids
     *  confirmed via a known show's `networks` field, not guessed), sorted by vote_average desc
     *  with the same [minVoteCount] floor as [discoverByGenre]. Powers broadcaster-based
     *  collections like "Best of ITV" (`discover/domain/UkCultureCollections.kt`). */
    suspend fun discoverByNetwork(networkId: Int, minVoteCount: Int): Result<List<TvShow>, DataError>
}
