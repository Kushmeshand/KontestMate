package com.example.kontestmate.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClientCF {
    private const val BASE_URL = "https://codeforces.com/api/"

    val api: ApiServiceCF by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiServiceCF::class.java)
    }
}
