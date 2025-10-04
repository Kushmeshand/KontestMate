package com.example.kontestmate.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = false)
    val index: String,
    val name: String,
    val contestName: String,
    val rating: Int?,
    val url: String, // ✅ <-- Add this field
    val platform: String,
)
fun Problem.toBookmarkEntity(platform: String): BookmarkEntity {
    return BookmarkEntity(
        index = this.index,
        name = this.name,
        contestName = this.contestName,
        rating = this.rating,
        url = this.url, // ✅ Add this line
        platform = platform
    )
}

