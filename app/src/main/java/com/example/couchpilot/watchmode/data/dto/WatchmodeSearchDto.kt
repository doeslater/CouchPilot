package com.example.couchpilot.watchmode.data.dto

import com.google.gson.annotations.SerializedName

data class WatchmodeSearchResponseDto(
    @SerializedName("title_results") val titleResults: List<WatchmodeSearchResultDto>?,
    @SerializedName("results") val results: List<WatchmodeSearchResultDto>?
)

data class WatchmodeSearchResultDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("result_type") val resultType: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("relevance") val relevance: Double?,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("tmdb_type") val tmdbType: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("user_rating") val userRating: Double?,
    @SerializedName("plot_overview") val plotOverview: String?
)
