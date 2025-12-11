package com.example.studybuddy

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// JSON looks like: { "slip": { "advice": "..." } }

data class Slip(
    val advice: String?
)

data class AdviceResponse(
    val slip: Slip?
)

interface AdviceApi {
    @GET("advice")
    suspend fun getAdvice(): AdviceResponse
}

object AdviceApiService {
    val api: AdviceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.adviceslip.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdviceApi::class.java)
    }
}
