package ru.andrey.whether.data

import kotlinx.serialization.Serializable

/** Ответ Open-Meteo archive API с суточными значениями. Все массивы параллельны по индексу. */
@Serializable
data class ArchiveResponse(
    val daily: DailyData? = null
)

@Serializable
data class DailyData(
    val time: List<String> = emptyList(),
    val temperature_2m_max: List<Double?> = emptyList(),
    val temperature_2m_min: List<Double?> = emptyList(),
    val temperature_2m_mean: List<Double?> = emptyList(),
    val precipitation_sum: List<Double?> = emptyList(),
    val wind_speed_10m_max: List<Double?> = emptyList(),
    val relative_humidity_2m_mean: List<Double?> = emptyList()
)

/** Один день, уже развёрнутый из параллельных массивов. */
data class DayRecord(
    val date: String,
    val tMax: Double?,
    val tMin: Double?,
    val tMean: Double?,
    val precipitation: Double?,
    val windMax: Double?,
    val humidityMean: Double?
)

fun DailyData.toRecords(): List<DayRecord> = time.mapIndexed { i, date ->
    DayRecord(
        date = date,
        tMax = temperature_2m_max.getOrNull(i),
        tMin = temperature_2m_min.getOrNull(i),
        tMean = temperature_2m_mean.getOrNull(i),
        precipitation = precipitation_sum.getOrNull(i),
        windMax = wind_speed_10m_max.getOrNull(i),
        humidityMean = relative_humidity_2m_mean.getOrNull(i)
    )
}
