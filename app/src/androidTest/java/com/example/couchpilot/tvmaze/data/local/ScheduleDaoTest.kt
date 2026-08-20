package com.example.couchpilot.tvmaze.data.local

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
class ScheduleDaoTest {

    private lateinit var database: CouchPilotDatabase
    private lateinit var dao: ScheduleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CouchPilotDatabase::class.java
        ).build()
        dao = database.scheduleDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetScheduleForDate() = runBlocking {
        val items = listOf(
            ScheduleItemEntity(1, 101, "Show 1", "Ep 1", "20:00", 30, "BBC One", null, null, null, "2024-08-20"),
            ScheduleItemEntity(2, 102, "Show 2", "Ep 2", "21:00", 60, "ITV1", null, null, null, "2024-08-20"),
            ScheduleItemEntity(3, 103, "Show 3", "Ep 3", "20:00", 30, "BBC One", null, null, null, "2024-08-21")
        )

        dao.insertScheduleItems(items)
        val result = dao.getScheduleForDate("2024-08-20").first()

        assertEquals(2, result.size)
        assertEquals("Show 1", result[0].showName)
        assertEquals("Show 2", result[1].showName)
    }

    @Test
    fun updatePosterUrl() = runBlocking {
        val item = ScheduleItemEntity(1, 101, "Show 1", "Ep 1", "20:00", 30, "BBC One", null, "imdb1", null, "2024-08-20")
        dao.insertScheduleItems(listOf(item))

        dao.updatePosterUrl(101, "https://poster.url")
        val result = dao.getScheduleForDate("2024-08-20").first()

        assertEquals("https://poster.url", result[0].posterUrl)
    }
}
