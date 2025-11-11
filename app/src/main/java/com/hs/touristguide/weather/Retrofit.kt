package com.hs.touristguide.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Retrofit API interface ---


// --- Singleton Retrofit instance ---
object RetrofitInstance {

    private const val BASE_URL = "https://api.openweathermap.org/"

    val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
