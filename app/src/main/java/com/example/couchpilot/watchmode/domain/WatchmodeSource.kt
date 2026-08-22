package com.example.couchpilot.watchmode.domain

data class WatchmodeSource(
    val name: String,
    val type: String,
    val webUrl: String,
    val format: String,
    val price: Double?
)
