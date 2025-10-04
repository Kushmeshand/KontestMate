package com.example.kontestmate

data class ApiContestItem(
    val event: String,
    val start: String,
    val end: String,
    val href: String,
    val resource: String // ✅ resource is a string like "Codeforces"
)
