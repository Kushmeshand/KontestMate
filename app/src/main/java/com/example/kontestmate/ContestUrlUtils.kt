package com.example.kontestmate.utils

fun getCorrectContestUrl(platform: String, contestName: String, originalUrl: String): String {
    return when (platform.lowercase()) {
        "codechef" -> {
            // Example: START97 becomes https://www.codechef.com/START97
            val code = contestName.trim().replace(" ", "")
            "https://www.codechef.com/$code"
        }

        "codeforces" -> {
            // Example: Codeforces Round #888 (Div. 3) -> https://codeforces.com/contests
            "https://codeforces.com/contests"
        }

        "leetcode" -> {
            val slug = contestName
                .lowercase()
                .replace(" ", "-")
                .replace(Regex("[^a-z0-9-]"), "")
            "https://leetcode.com/contest/$slug"
        }

        else -> originalUrl // fallback
    }
}
