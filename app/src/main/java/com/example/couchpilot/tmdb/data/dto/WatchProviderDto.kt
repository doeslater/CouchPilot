package com.example.couchpilot.tmdb.data.dto

import com.google.gson.annotations.SerializedName

data class WatchProvidersResponseDto(
    val results: List<WatchProviderDto> = emptyList()
)

data class WatchProviderDto(
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("display_priority") val displayPriority: Int
)
