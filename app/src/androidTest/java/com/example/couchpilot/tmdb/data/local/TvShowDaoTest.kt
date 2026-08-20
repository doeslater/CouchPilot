package com.example.couchpilot.tmdb.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couchpilot.core.database.CouchPilotDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvShowDaoTest {

    private lateinit var database: CouchPilotDatabase
    private lateinit var dao: TvShowDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CouchPilotDatabase::class.java
        ).build()
        dao = database.tvShowDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAllShows() = runBlocking {
        val shows = listOf(
            TvShowEntity(1, "Show 1", "Overview 1", null, 8.0, "2024-01-01"),
            TvShowEntity(2, "Show 2", "Overview 2", null, 7.5, "2024-02-01")
        )

        dao.insertTvShows(shows)
        val result = dao.getAllTvShows().first()

        assertEquals(2, result.size)
        assertEquals("Show 1", result[0].name)
        assertEquals("Show 2", result[1].name)
    }

    @Test
    fun getShowById() = runBlocking {
        val show = TvShowEntity(1, "Show 1", "Overview 1", null, 8.0, "2024-01-01")
        dao.insertTvShows(listOf(show))

        val result = dao.getTvShowById(1)
        assertEquals("Show 1", result?.name)
    }
}
