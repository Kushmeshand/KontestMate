package com.example.kontestmate.network.model
data class CListResponse(
    val objects: List<ContestObject>
)
data class ContestObject(
    val duration: Int,
    val end: String,
    val event: String,
    val host: String,
    val href: String,
    val id: Int,
    val n_problems: Int,
    val n_statistics: Int,
    val parsed_at: String,
    val problems: Any?, // this is `null`, so keep as nullable
    val resource: String, // ✅ string like "leetcode.com"
    val resource_id: Int,
    val start: String
)
