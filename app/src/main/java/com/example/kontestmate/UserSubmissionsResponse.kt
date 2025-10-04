package com.example.kontestmate.models

data class UserSubmissionsResponse(
    val status: String,
    val result: List<Submission>
)

data class Submission(
    val id: Int,
    val problem: Problemnew,
    val verdict: String?
)

data class Problemnew(
    val contestId: Int?,
    val index: String
)
