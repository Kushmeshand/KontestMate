package com.example.kontestmate.network

import com.example.kontestmate.network.model.CListResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ContestApiService {
    @GET("contest/")
    fun getContests(
        @Query("username") username: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 50,
        @Query("order_by") orderBy: String = "start"
    ): Call<CListResponse>
}
