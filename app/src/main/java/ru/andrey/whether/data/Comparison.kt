package ru.andrey.whether.data

/** Метрики, по которым можно сравнивать периоды. */
enum class Metric(val label: String, val unit: String, val pick: (DayRecord) -> Double?) {
    T_MIN("Минимум", "°C", { it.tMin }),
    T_MEAN("Средняя", "°C", { it.tMean }),
    T_MAX("Максимум", "°C", { it.tMax }),
    PRECIPITATION("Осадки", "мм", { it.precipitation }),
    WIND("Ветер макс.", "км/ч", { it.windMax }),
    HUMIDITY("Влажность", "%", { it.humidityMean })
}

/** Один загруженный период: подпись, даты и дневные записи. */
data class PeriodSeries(
    val label: String,
    val startDate: String,
    val endDate: String,
    val days: List<DayRecord>
) {
    /** Значения выбранной метрики по дням, без пропусков (null отбрасываются). */
    fun values(metric: Metric): List<Double> = days.mapNotNull(metric.pick)

    fun average(metric: Metric): Double? = values(metric).takeIf { it.isNotEmpty() }?.average()
    fun min(metric: Metric): Double? = values(metric).minOrNull()
    fun max(metric: Metric): Double? = values(metric).maxOrNull()
}

/**
 * Сопоставление периодов по дням: первый день первого периода с первым днём второго и т. д.
 * Возвращает для каждого периода список значений по индексу дня (null, если данных нет).
 */
fun alignByDay(series: List<PeriodSeries>, metric: Metric): List<List<Double?>> {
    val length = series.maxOfOrNull { it.days.size } ?: 0
    return series.map { s ->
        List(length) { i -> s.days.getOrNull(i)?.let(metric.pick) }
    }
}
