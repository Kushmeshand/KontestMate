package com.example.kontestmate.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call
import com.example.kontestmate.models.*

interface ApiServiceCF {

    @GET("user.info")
    fun getUserInfo(@Query("handles") handle: String): Call<UserInfoResponse>

    @GET("user.status")
    fun getUserStatus(@Query("handle") handle: String): Call<UserSubmissionsResponse>
}
