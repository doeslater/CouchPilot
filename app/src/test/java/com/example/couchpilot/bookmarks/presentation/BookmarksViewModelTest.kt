package com.example.couchpilot.bookmarks.presentation

import com.example.couchpilot.bookmarks.data.local.BookmarkDao
import com.example.couchpilot.bookmarks.data.local.BookmarkEntity
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {

    private val bookmarkDao: BookmarkDao = mockk()
    private val tmdbRepository: TmdbRepository = mockk()

    private fun show(id: Int) = TvShow(
        id = id,
        name = "Show $id",
        overview = "overview",
        posterUrl = null,
        voteAverage = 8.0,
        firstAirDate = "2024",
        genreIds = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `no bookmarks yields an empty success state`() {
        every { bookmarkDao.getAllBookmarks() } returns flowOf(emptyList())

        val viewModel = BookmarksViewModel(bookmarkDao, tmdbRepository)

        val state = viewModel.uiState.value as BookmarksUiState.Success
        assertTrue(state.shows.isEmpty())
    }

    @Test
    fun `bookmarked shows are hydrated via TMDB in bookmark order`() {
        every { bookmarkDao.getAllBookmarks() } returns flowOf(
            listOf(BookmarkEntity(showId = 2), BookmarkEntity(showId = 1))
        )
        coEvery { tmdbRepository.getTvShowById(1) } returns Result.Success(show(1))
        coEvery { tmdbRepository.getTvShowById(2) } returns Result.Success(show(2))

        val viewModel = BookmarksViewModel(bookmarkDao, tmdbRepository)

        val state = viewModel.uiState.value as BookmarksUiState.Success
        assertEquals(listOf(2, 1), state.shows.map { it.id })
    }

    @Test
    fun `a show that fails to hydrate is dropped rather than failing the whole list`() {
        every { bookmarkDao.getAllBookmarks() } returns flowOf(
            listOf(BookmarkEntity(showId = 1), BookmarkEntity(showId = 2))
        )
        coEvery { tmdbRepository.getTvShowById(1) } returns Result.Error(DataError.Network.UNKNOWN)
        coEvery { tmdbRepository.getTvShowById(2) } returns Result.Success(show(2))

        val viewModel = BookmarksViewModel(bookmarkDao, tmdbRepository)

        val state = viewModel.uiState.value as BookmarksUiState.Success
        assertEquals(listOf(2), state.shows.map { it.id })
    }
}
