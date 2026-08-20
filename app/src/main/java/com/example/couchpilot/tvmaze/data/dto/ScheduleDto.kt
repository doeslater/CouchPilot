package com.example.couchpilot.tvmaze.data.dto


data class EpisodeDto(
    val id: Int,
    val url: String?,
    val name: String?,
    val season: Int?,
    val number: Int?,
    val type: String?,
    val airdate: String?,
    val airtime: String?,
    val airstamp: String?,
    val runtime: Int?,
    val rating: RatingDto?,
    val image: ImageDto?,
    val summary: String?,
    val show: TvMazeShowDto
)

data class TvMazeShowDto(
    val id: Int,
    val url: String?,
    val name: String?,
    val type: String?,
    val language: String?,
    val genres: List<String>?,
    val status: String?,
    val runtime: Int?,
    val premiered: String?,
    val ended: String?,
    val officialSite: String?,
    val schedule: TvMazeScheduleDto?,
    val rating: RatingDto?,
    val weight: Int?,
    val network: NetworkDto?,
    val webChannel: NetworkDto?,
    val externals: ExternalsDto?,
    val image: ImageDto?,
    val summary: String?
)

data class TvMazeScheduleDto(
    val time: String?,
    val days: List<String>?
)

data class NetworkDto(
    val id: Int,
    val name: String?,
    val country: CountryDto?
)

data class CountryDto(
    val name: String?,
    val code: String?,
    val timezone: String?
)

data class ExternalsDto(
    val tvrage: Int?,
    val thetvdb: Int?,
    val imdb: String?
)

data class RatingDto(
    val average: Double?
)

data class ImageDto(
    val medium: String?,
    val original: String?
)
