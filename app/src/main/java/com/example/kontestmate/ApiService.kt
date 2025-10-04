package com.example.kontestmate.api

import com.example.kontestmate.models.ContestStandingsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("contest.standings")
    suspend fun getContestProblems(
        @Query("contestId") contestId: Int,
        @Query("from") from: Int = 1,
        @Query("count") count: Int = 1000,
        @Query("showUnofficial") showUnofficial: Boolean = true
    ): ContestStandingsResponse

}
