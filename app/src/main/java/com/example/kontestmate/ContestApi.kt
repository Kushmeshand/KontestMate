package com.example.kontestmate

import retrofit2.Call
import retrofit2.http.GET

interface ContestApi {
    @GET("all")
    fun getAllContests(): Call<List<ApiContestItem>>
}