package com.hs.touristguide.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- API Interface ---
interface WeatherApiService {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

// --- Repository-like Object (inside same file) ---
object WeatherApi {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val API_KEY = "3297f7df33af68844aa1c77fa7c255e8" // replace with your API key

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }

    suspend fun getWeather(city: String): WeatherResponse {
        return api.getWeather(city, API_KEY)
    }
}
