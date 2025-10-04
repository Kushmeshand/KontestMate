package com.example.kontestmate.models

data class UserInfoResponse(
    val status: String,
    val result: List<CodeforcesUser>
)

data class CodeforcesUser(
    val handle: String,
    val rating: Int?,
    val maxRating: Int?,
    val rank: String?,
    val maxRank: String?
)
