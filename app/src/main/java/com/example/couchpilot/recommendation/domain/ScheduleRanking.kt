package com.example.couchpilot.recommendation.domain

import com.example.couchpilot.tvmaze.domain.ScheduleItem

/**
 * Ranks by TVmaze's own show rating, descending, unrated shows last. This is a stand-in for
 * ROADMAP.md Phase 5's real preference-based scorer - there's no swipe/watch-history signal yet
 * (Phases 4/5 haven't landed), so "recommended" today just means "well-rated," not personalized.
 * Phase 5's cosine-similarity scorer should replace (or blend with) this, not sit next to it.
 */
fun List<ScheduleItem>.rankedByRating(): List<ScheduleItem> =
    sortedByDescending { it.rating ?: Double.NEGATIVE_INFINITY }
