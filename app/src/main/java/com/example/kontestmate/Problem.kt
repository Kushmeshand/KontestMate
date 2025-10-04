package com.example.kontestmate.models

data class Problem(
    val index: String,
    val name: String,
    val rating: Int?,
    val contestName: String,
    val platform: String,
    val url: String,  // ✅ Add this line
    val tags: List<String>,// ✅ Add this line
    var isBookmarked: Boolean = false,
)


