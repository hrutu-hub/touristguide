package com.hs.touristguide.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hs.touristguide.ml.WeatherMLAnalyzer
import com.hs.touristguide.weather.WeatherData
import com.hs.touristguide.weather.WeatherResponse
import com.hs.touristguide.weather.toWeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class WeatherCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val mlAnalyzer = WeatherMLAnalyzer(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get user’s last selected city or default
            val sharedPrefs = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val city = sharedPrefs.getString("last_city", "Mumbai") ?: "Mumbai"

            // Fetch weather
            val weather = fetchWeather(city)

            // Run ML and send notification if allowed
            mlAnalyzer.analyzeAndNotify(weather)

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    private suspend fun fetchWeather(city: String): WeatherData {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherApi::class.java)
        val apiKey = "YOUR_OPENWEATHERMAP_KEY" // replace with your key
        val response = api.getWeather(city, apiKey, "metric")
        return response.toWeatherData()
    }

    interface WeatherApi {
        @GET("data/2.5/weather")
        suspend fun getWeather(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String
        ): WeatherResponse
    }
}
