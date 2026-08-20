package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.tvmaze.data.dto.EpisodeDto
import com.example.couchpilot.tvmaze.domain.ScheduleItem

import com.example.couchpilot.tvmaze.data.local.ScheduleItemEntity

fun EpisodeDto.toScheduleItem(): ScheduleItem {
    return ScheduleItem(
        id = id,
        showId = show.id,
        showName = show.name ?: "Unknown Show",
        episodeName = name,
        airtime = airtime,
        runtime = runtime,
        channel = show.network?.name ?: show.webChannel?.name,
        summary = summary,
        imdbId = show.externals?.imdb,
        rating = show.rating?.average,
    )
}

fun EpisodeDto.toEntity(date: String): ScheduleItemEntity {
    return ScheduleItemEntity(
        id = id,
        showId = show.id,
        showName = show.name ?: "Unknown Show",
        episodeName = name,
        airtime = airtime,
        runtime = runtime,
        channel = show.network?.name ?: show.webChannel?.name,
        summary = summary,
        imdbId = show.externals?.imdb,
        posterUrl = null, // Enriched later
        date = date,
        rating = show.rating?.average,
    )
}

fun ScheduleItemEntity.toScheduleItem(): ScheduleItem {
    return ScheduleItem(
        id = id,
        showId = showId,
        showName = showName,
        episodeName = episodeName,
        airtime = airtime,
        runtime = runtime,
        channel = channel,
        summary = summary,
        imdbId = imdbId,
        posterUrl = posterUrl,
        rating = rating,
    )
}
