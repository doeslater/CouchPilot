package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.tvmaze.data.dto.EpisodeDto
import com.example.couchpilot.tvmaze.domain.ScheduleItem

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
        imdbId = show.externals?.imdb
    )
}
