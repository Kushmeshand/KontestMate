package com.example.kontestmate.network

import com.example.kontestmate.network.model.CListResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CListApiService {

    @GET("contest/")
    fun getUpcomingContests(
        @Query("username") username: String,
        @Query("api_key") apiKey: String,
        @Query("start__gte") startTime: String,
        @Query("order_by") orderBy: String,
        @Query("resource_id__in") resourceIds: String
    ): Call<CListResponse>

    @GET("contest/")
    fun getPastContests(
        @Query("username") username: String,
        @Query("api_key") apiKey: String,
        @Query("end__lt") endTime: String,
        @Query("order_by") orderBy: String,
        @Query("resource_id__in") resourceIds: String
    ): Call<CListResponse>

}

