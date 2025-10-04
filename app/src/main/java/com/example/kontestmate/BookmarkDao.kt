package com.example.kontestmate.data

import androidx.room.*
import com.example.kontestmate.models.BookmarkEntity

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks WHERE url = :problemUrl")
    suspend fun deleteByUrl(problemUrl: String)

    @Query("SELECT * FROM bookmarks WHERE `index` = :index AND contestName = :contestName")
    suspend fun getByIndex(index: String, contestName: String): BookmarkEntity?
}
