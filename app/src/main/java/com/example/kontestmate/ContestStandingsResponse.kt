package com.example.kontestmate.models

data class ContestStandingsResponse(
    val status: String,
    val result: ContestResult
)

data class ContestResult(
    val contest: Contest,
    val problems: List<ApiProblem>
)

data class Contest(
    val id: Int,
    val name: String
)

data class ApiProblem(
    val index: String,
    val name: String,
    val rating: Int? = null,
    val tags: List<String>,
    val contestId: Int
)

