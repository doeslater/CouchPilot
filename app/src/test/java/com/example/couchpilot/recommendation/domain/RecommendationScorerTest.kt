package com.example.couchpilot.recommendation.domain

import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationScorerTest {

    private lateinit var scorer: RecommendationScorer
    private val swipeEventDao: SwipeEventDao = mockk()

    @Before
    fun setup() {
        scorer = RecommendationScorer(swipeEventDao)
    }

    @Test
    fun `score returns higher for liked genres`() = runBlocking {
        // User likes Sci-Fi (id: 10765) and dislikes Reality (id: 10764)
        val events = listOf(
            SwipeEventEntity(showId = 1, genreIds = "10765", liked = true),
            SwipeEventEntity(showId = 2, genreIds = "10764", liked = false)
        )
        coEvery { swipeEventDao.getAllSwipeEvents() } returns flowOf(events)

        val userTaste = scorer.computePreferenceVector()

        val sciFiScore = scorer.score(listOf(10765), userTaste)
        val realityScore = scorer.score(listOf(10764), userTaste)
        val mysteryScore = scorer.score(listOf(9648), userTaste)

        assertTrue("Sci-Fi should score higher than Mystery", sciFiScore > mysteryScore)
        assertTrue("Mystery should score higher than Reality", mysteryScore > realityScore)
        assertEquals(0.5, mysteryScore, 0.01) // Neutral overlap
    }
}
