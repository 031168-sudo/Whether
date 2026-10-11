package ru.andrey.whether.data

/**
 * Загрузка истории. Кэш в Room пока не подключён: это следующий шаг.
 * Сейчас каждый запрос идёт в сеть.
 */
class WeatherRepository(private val api: OpenMeteoArchiveApi = OpenMeteoArchiveApi.create()) {

    suspend fun history(
        latitude: Double,
        longitude: Double,
        startDate: String,
        endDate: String
    ): List<DayRecord> {
        val response = api.getDaily(latitude, longitude, startDate, endDate)
        return response.daily?.toRecords().orEmpty()
    }
}
