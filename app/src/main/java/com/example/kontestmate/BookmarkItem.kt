package com.example.kontestmate

data class BookmarkItem(
    val problemName: String,
    val platformName: String,
    val contestName: String,
    val problemUrl: String?,
    var isBookmarked: Boolean = true
)


