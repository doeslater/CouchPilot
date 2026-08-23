package com.example.couchpilot.bookmarks.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.couchpilot.core.database.CouchPilotDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

    private lateinit var database: CouchPilotDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CouchPilotDatabase::class.java
        ).build()
        dao = database.bookmarkDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAllBookmarks() = runBlocking {
        dao.insertBookmark(BookmarkEntity(showId = 1))

        val result = dao.getAllBookmarks().first()

        assertEquals(1, result.size)
        assertEquals(1, result[0].showId)
    }

    @Test
    fun getBookmarkReturnsNullWhenNotBookmarked() = runBlocking {
        val result = dao.getBookmark(1)

        assertNull(result)
    }

    @Test
    fun getBookmarkReturnsTheRowOnceInserted() = runBlocking {
        dao.insertBookmark(BookmarkEntity(showId = 1))

        val result = dao.getBookmark(1)

        assertNotNull(result)
        assertEquals(1, result?.showId)
    }

    @Test
    fun deleteBookmarkRemovesOnlyThatShow() = runBlocking {
        dao.insertBookmark(BookmarkEntity(showId = 1))
        dao.insertBookmark(BookmarkEntity(showId = 2))

        dao.deleteBookmark(1)

        assertNull(dao.getBookmark(1))
        assertNotNull(dao.getBookmark(2))
    }

    @Test
    fun clearAllRemovesEveryBookmark() = runBlocking {
        dao.insertBookmark(BookmarkEntity(showId = 1))
        dao.insertBookmark(BookmarkEntity(showId = 2))

        dao.clearAll()

        val result = dao.getAllBookmarks().first()
        assertTrue(result.isEmpty())
    }
}
