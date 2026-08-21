package com.example.couchpilot.tmdb.data

import com.example.couchpilot.tmdb.data.dto.TvShowDetailDto
import com.example.couchpilot.tmdb.data.dto.TvShowDto
import com.example.couchpilot.tmdb.data.dto.WatchProviderDto
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

import com.example.couchpilot.tmdb.data.local.TvShowEntity

fun TvShowDto.toTvShow(): TvShow = TvShow(
    id = id,
    name = name,
    overview = overview,
    posterUrl = TmdbImages.posterUrl(posterPath),
    voteAverage = voteAverage,
    firstAirDate = firstAirDate,
    genreIds = genreIds
)

fun TvShowDto.toEntity(): TvShowEntity = TvShowEntity(
    id = id,
    name = name,
    overview = overview,
    posterUrl = TmdbImages.posterUrl(posterPath),
    voteAverage = voteAverage,
    firstAirDate = firstAirDate,
    genreIds = genreIds.joinToString(",")
)

fun TvShowEntity.toTvShow(): TvShow = TvShow(
    id = id,
    name = name,
    overview = overview,
    posterUrl = posterUrl,
    voteAverage = voteAverage,
    firstAirDate = firstAirDate,
    genreIds = if (genreIds.isBlank()) emptyList() else genreIds.split(",").map { it.toInt() }
)

fun TvShowDetailDto.toTvShow(): TvShow = TvShow(
    id = id,
    name = name,
    overview = overview,
    posterUrl = TmdbImages.posterUrl(posterPath),
    voteAverage = voteAverage,
    firstAirDate = firstAirDate,
    genreIds = genres.map { it.id }
)

fun TvShowDetailDto.toEntity(): TvShowEntity = TvShowEntity(
    id = id,
    name = name,
    overview = overview,
    posterUrl = TmdbImages.posterUrl(posterPath),
    voteAverage = voteAverage,
    firstAirDate = firstAirDate,
    genreIds = genres.map { it.id }.joinToString(",")
)

fun WatchProviderDto.toWatchProvider(tmdbUrl: String? = null): WatchProvider = WatchProvider(
    id = providerId,
    name = providerName,
    logoUrl = TmdbImages.logoUrl(logoPath),
    tmdbUrl = tmdbUrl
)
