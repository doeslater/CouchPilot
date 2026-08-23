package com.example.couchpilot.bookmarks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE showId = :showId")
    suspend fun deleteBookmark(showId: Int)

    @Query("SELECT * FROM bookmarks WHERE showId = :showId")
    suspend fun getBookmark(showId: Int): BookmarkEntity?

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()
}
