package com.hs.touristguide.weather

data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int
)

data class Weather(
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double
)

// ✅ ADD: Extension function to convert to simplified WeatherData for ML
fun WeatherResponse.toWeatherData(): WeatherData {
    return WeatherData(
        temperature = this.main.temp.toFloat(),
        condition = this.weather.firstOrNull()?.description ?: "Unknown",
        humidity = this.main.humidity.toFloat(),
        windSpeed = this.wind.speed.toFloat(),
        timestamp = System.currentTimeMillis()
    )
}

// ✅ ADD: Simplified data class for ML processing
data class WeatherData(
    val temperature: Float,
    val condition: String,
    val humidity: Float,
    val windSpeed: Float,
    val timestamp: Long
)