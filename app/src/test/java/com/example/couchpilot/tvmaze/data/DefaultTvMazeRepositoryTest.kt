package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tvmaze.data.local.ScheduleDao
import com.example.couchpilot.tvmaze.data.local.ScheduleItemEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultTvMazeRepositoryTest {

    private lateinit var repository: DefaultTvMazeRepository
    private val remoteDataSource: RetrofitTvMazeRemoteDataSource = mockk()
    private val scheduleDao: ScheduleDao = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = DefaultTvMazeRepository(remoteDataSource, scheduleDao)
    }

    @Test
    fun `getScheduleForDate returns cache if present`() = runBlocking {
        val date = "2024-08-20"
        val cached = listOf(
            ScheduleItemEntity(1, 101, "Cached Show", null, null, null, null, null, null, null, date)
        )
        coEvery { scheduleDao.getScheduleForDate(date) } returns flowOf(cached)

        val result = repository.getScheduleForDate(date)

        assertTrue(result is Result.Success)
        assertEquals("Cached Show", (result as Result.Success).data[0].showName)
        coVerify(exactly = 0) { remoteDataSource.getSchedule(date = any()) }
    }
}
