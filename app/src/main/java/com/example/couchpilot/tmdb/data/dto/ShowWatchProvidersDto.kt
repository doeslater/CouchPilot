package com.example.couchpilot.tmdb.data.dto

import com.google.gson.annotations.SerializedName

data class ShowWatchProvidersResponseDto(
    val id: Int,
    val results: Map<String, WatchProviderRegionDto> = emptyMap()
)

data class WatchProviderRegionDto(
    val link: String?,
    val flatrate: List<WatchProviderDto>? = emptyList(),
    val buy: List<WatchProviderDto>? = emptyList(),
    val rent: List<WatchProviderDto>? = emptyList()
)
