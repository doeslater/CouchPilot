package com.example.couchpilot.watchmode.data.dto

import com.google.gson.annotations.SerializedName

data class WatchmodeSourceDto(
    @SerializedName("source_id") val sourceId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String, // sub, rent, buy, free
    @SerializedName("region") val region: String,
    @SerializedName("web_url") val webUrl: String,
    @SerializedName("format") val format: String, // HD, SD, 4K
    @SerializedName("price") val price: Double?
)
