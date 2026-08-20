package com.example.couchpilot.tvmaze.domain

object FreeviewChannels {
    val whitelist = setOf(
        "BBC One",
        "BBC Two",
        "BBC Three",
        "BBC Four",
        "BBC News",
        "ITV1",
        "ITV2",
        "ITV3",
        "ITV4",
        "ITVBe",
        "Channel 4",
        "E4",
        "More4",
        "4Seven",
        "Film4",
        "Channel 5",
        "5USA",
        "5STAR",
        "5Action",
        "My5",
        "Yesterday",
        "Dave",
        "Drama",
        "W",
        "Quest",
        "Quest Red",
        "Really",
        "HGTV",
        "Food Network",
        "DMAX"
    )

    fun isFreeview(channelName: String?): Boolean {
        if (channelName == null) return false
        val normalized = channelName.trim()
        // Exact match, or a regional-variant prefix (e.g. "BBC One London" / "BBC One Wales").
        // Deliberately not a raw substring `contains` check - that let short entries like "W"
        // match anything containing the letter w (e.g. "Sky Witness", "The CW").
        return whitelist.any { entry ->
            normalized.equals(entry, ignoreCase = true) ||
                normalized.startsWith("$entry ", ignoreCase = true)
        }
    }
}
