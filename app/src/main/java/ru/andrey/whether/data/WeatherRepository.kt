package ru.andrey.whether.data

import ru.andrey.whether.data.db.DailyDao
import ru.andrey.whether.data.db.toEntity
import ru.andrey.whether.data.db.toRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Загрузка истории с кэшем в Room.
 *
 * Кэшируются только дни, которые старше [STABLE_LAG_DAYS]: реанализ Open-Meteo
 * дополняется с задержкой, и свежие значения могут поменяться.
 * Если все дни диапазона уже в кэше, сеть не используется.
 */
class WeatherRepository(
    private val api: OpenMeteoArchiveApi,
    private val dao: DailyDao,
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

    companion object {
        /** Сколько последних дней не кэшировать. */
        const val STABLE_LAG_DAYS = 7L

        /** Округление координат до 0.01° и формат с точкой, а не запятой. */
        fun cacheKey(latitude: Double, longitude: Double): String =
            String.format(Locale.US, "%.2f,%.2f", latitude, longitude)
    }
}
