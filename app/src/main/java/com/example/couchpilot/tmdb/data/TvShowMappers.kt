package com.example.couchpilot.tmdb.data

import com.example.couchpilot.tmdb.data.dto.TvShowDto
import com.example.couchpilot.tmdb.domain.TvShow

fun TvShowDto.toTvShow(): TvShow = TvShow(
    id = id,
    name = name,
    overview = overview,
    posterUrl = TmdbImages.posterUrl(posterPath),
    voteAverage = voteAverage,
    firstAirDate = firstAirDate,
)
