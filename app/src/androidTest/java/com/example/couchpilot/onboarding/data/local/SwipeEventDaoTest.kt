package com.example.couchpilot.onboarding.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couchpilot.core.database.CouchPilotDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeEventDaoTest {

    private lateinit var database: CouchPilotDatabase
    private lateinit var dao: SwipeEventDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CouchPilotDatabase::class.java
        ).build()
        dao = database.swipeEventDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAllEvents() = runBlocking {
        val event = SwipeEventEntity(
            showId = 1,
            genreIds = "10,20",
            liked = true
        )

        dao.insertSwipeEvent(event)
        val result = dao.getAllSwipeEvents().first()

        assertEquals(1, result.size)
        assertEquals(1, result[0].showId)
        assertTrue(result[0].liked)
    }

    @Test
    fun clearAllEvents() = runBlocking {
        dao.insertSwipeEvent(SwipeEventEntity(showId = 1, genreIds = "", liked = true))
        dao.clearAll()
        val result = dao.getAllSwipeEvents().first()
        assertTrue(result.isEmpty())
    }
}
