package ru.andrey.whether.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Прогноз Open-Meteo. Часть переменных в прогнозе недоступна (например, средняя влажность),
 * поэтому запрашиваем только то, что есть: максимум, минимум, осадки, ветер.
 */
interface OpenMeteoForecastApi {

    @GET("v1/forecast")
    suspend fun getDaily(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = FORECAST_DAILY_PARAMS,
        @Query("forecast_days") forecastDays: Int = 16,
        @Query("timezone") timezone: String = "auto"
    ): ArchiveResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        const val FORECAST_DAILY_PARAMS =
            "temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max"

        fun create(): OpenMeteoForecastApi {
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenMeteoForecastApi::class.java)
        }
    }
}
