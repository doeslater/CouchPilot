package com.example.couchpilot.tmdb.data

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
)

fun TvShowDto.toEntity(): TvShowEntity = TvShowEntity(
    id = id,
    name = name,
    overview = overview,
    posterUrl = TmdbImages.posterUrl(posterPath),
    voteAverage = voteAverage,
    firstAirDate = firstAirDate
)

fun TvShowEntity.toTvShow(): TvShow = TvShow(
    id = id,
    name = name,
    overview = overview,
    posterUrl = posterUrl,
    voteAverage = voteAverage,
    firstAirDate = firstAirDate
)

fun WatchProviderDto.toWatchProvider(): WatchProvider = WatchProvider(
    id = providerId,
    name = providerName,
    logoUrl = TmdbImages.logoUrl(logoPath)
)
