package com.example.couchpilot.tmdb.data.dto

import com.google.gson.annotations.SerializedName

data class FindByIdResponseDto(
    @SerializedName("tv_results")
    val tvResults: List<TvShowDto> = emptyList()
)
