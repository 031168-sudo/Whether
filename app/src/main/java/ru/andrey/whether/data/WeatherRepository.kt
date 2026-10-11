package ru.andrey.whether.data

import ru.andrey.whether.data.db.DailyDao
import ru.andrey.whether.data.db.toEntity
import ru.andrey.whether.data.db.toRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Прогноз и история.
 *
 * История кэшируется в Room. Кэшируются только дни старше [STABLE_LAG_DAYS]:
 * реанализ Open-Meteo дополняется с задержкой, и свежие значения могут поменяться.
 * Прогноз не кэшируется: он меняется каждый час.
 */
class WeatherRepository(
    private val api: OpenMeteoArchiveApi,
    private val dao: DailyDao,
    private val forecastApi: OpenMeteoForecastApi = OpenMeteoForecastApi.create(),
    private val today: () -> LocalDate = { LocalDate.now() }
) {

    suspend fun history(
        latitude: Double,
        longitude: Double,
        startDate: String,
        endDate: String
    ): List<DayRecord> {
        val key = cacheKey(latitude, longitude)
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        require(!end.isBefore(start)) { "Дата «по» раньше даты «с»" }

        val cacheable = end <= today().minusDays(STABLE_LAG_DAYS)

        if (cacheable) {
            val expected = ChronoUnit.DAYS.between(start, end).toInt() + 1
            val cached = dao.range(key, startDate, endDate)
            if (cached.size == expected) return cached.map { it.toRecord() }
        }

        val fresh = api.getDaily(latitude, longitude, startDate, endDate)
            .daily?.toRecords().orEmpty()

        if (cacheable && fresh.isNotEmpty()) {
            dao.upsertAll(fresh.map { it.toEntity(key) })
        }
        return fresh
    }

    /** Прогноз на 16 дней. Средняя температура в прогнозе не приходит, считаем её как среднее max и min. */
    suspend fun forecast(latitude: Double, longitude: Double): List<DayRecord> {
        val daily = forecastApi.getDaily(latitude, longitude).daily ?: return emptyList()
        return daily.toRecords().map { r ->
            if (r.tMean != null || r.tMax == null || r.tMin == null) r
            else r.copy(tMean = (r.tMax + r.tMin) / 2)
        }
    }

    /** Удаляет все кэшированные дни для всех координат. */
    suspend fun clearCache() = dao.deleteAll()

    companion object {
        /** Сколько последних дней не кэшировать. */
        const val STABLE_LAG_DAYS = 7L

        /** Округление координат до 0.01° и формат с точкой, а не запятой. */
        fun cacheKey(latitude: Double, longitude: Double): String =
            String.format(Locale.US, "%.2f,%.2f", latitude, longitude)
    }
}
