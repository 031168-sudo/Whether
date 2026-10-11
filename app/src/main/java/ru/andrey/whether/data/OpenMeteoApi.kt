package ru.andrey.whether.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

interface OpenMeteoArchiveApi {

    @GET("v1/archive")
    suspend fun getDaily(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = DAILY_PARAMS,
        @Query("timezone") timezone: String = "auto"
    ): ArchiveResponse

    companion object {
        const val BASE_URL = "https://archive-api.open-meteo.com/"

        const val DAILY_PARAMS =
            "temperature_2m_max,temperature_2m_min,temperature_2m_mean," +
                "precipitation_sum,wind_speed_10m_max,relative_humidity_2m_mean"

        fun create(): OpenMeteoArchiveApi {
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenMeteoArchiveApi::class.java)
        }
    }
}
