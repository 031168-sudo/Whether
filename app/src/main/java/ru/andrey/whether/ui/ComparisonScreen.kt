package ru.andrey.whether.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.andrey.whether.data.Metric
import ru.andrey.whether.data.PeriodSeries
import ru.andrey.whether.data.WeatherRepository
import ru.andrey.whether.data.alignByDay

private const val MAX_PERIODS = 4

/** Редактируемое описание периода. */
class PeriodInput(label: String, start: String, end: String) {
    var label by mutableStateOf(label)
    var start by mutableStateOf(start)
    var end by mutableStateOf(end)
}

private val seriesColors = listOf(
    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFB8C00)
)

@Composable
fun ComparisonScreen(repository: WeatherRepository, coords: CoordinatesState) {
    val scope = rememberCoroutineScope()

    val periods = remember {
        mutableStateListOf(
            PeriodInput("2024, ноябрь", "2024-11-01", "2024-11-30"),
            PeriodInput("2025, ноябрь", "2025-11-01", "2025-11-30")
        )
    }
    var metric by remember { mutableStateOf(Metric.T_MEAN) }
    var loaded by remember { mutableStateOf<List<PeriodSeries>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun compare() {
        val c = coords.parse()
        if (c == null) {
            error = "Проверьте широту (-90..90) и долготу (-180..180)"
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                // Периоды грузятся параллельно.
                loaded = coroutineScope {
                    periods.map { p ->
                        async {
                            PeriodSeries(
                                label = p.label,
                                startDate = p.start,
                                endDate = p.end,
                                days = repository.history(c.latitude, c.longitude, p.start, p.end)
                            )
                        }
                    }.map { it.await() }
                }
                if (loaded.all { it.days.isEmpty() }) error = "Нет данных ни за один период"
            } catch (e: Exception) {
                error = "Не удалось загрузить данные: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Сравнение периодов", style = MaterialTheme.typography.headlineSmall)

        CoordinatesInput(coords) { error = it }

        periods.forEachIndexed { index, p ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = p.label, onValueChange = { p.label = it },
                    label = { Text("Название периода ${index + 1}") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = p.start, onValueChange = { p.start = it },
                        label = { Text("С") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = p.end, onValueChange = { p.end = it },
                        label = { Text("По") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                if (periods.size > 2) {
                    OutlinedButton(onClick = { periods.removeAt(index) }) {
                        Text("Удалить период")
                    }
                }
            }
        }

        if (periods.size < MAX_PERIODS) {
            OutlinedButton(onClick = {
                periods.add(PeriodInput("Период ${periods.size + 1}", "", ""))
            }) {
                Text("Добавить период (до $MAX_PERIODS)")
            }
        }

        MetricChips(metric) { metric = it }

        Button(onClick = { compare() }, enabled = !loading) {
            Text("Сравнить")
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (loaded.any { it.days.isNotEmpty() }) {
            Text("${metric.label}, ${metric.unit}", style = MaterialTheme.typography.labelLarge)
            val aligned = alignByDay(loaded, metric)
            val length = aligned.maxOf { it.size }
            LineChart(
                series = loaded.mapIndexed { s, _ -> seriesColors[s % seriesColors.size] to aligned[s] },
                xStartLabel = "1-й день",
                xEndLabel = "$length-й день"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                loaded.forEachIndexed { s, p ->
                    Text(
                        "● ${p.label}",
                        color = seriesColors[s % seriesColors.size],
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            SummaryTable(loaded, metric)
        }
    }
}

/** Сводка по каждому периоду и разница со вторым, то есть с базовым. */
@Composable
private fun SummaryTable(series: List<PeriodSeries>, metric: Metric) {
    val base = series.first().average(metric)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Период", modifier = Modifier.weight(1.6f), style = MaterialTheme.typography.labelMedium)
            Text("Среднее", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Мин", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Макс", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Δ к 1-му", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        }
        series.forEachIndexed { index, p ->
            val avg = p.average(metric)
            val diff = if (index == 0 || avg == null || base == null) null else avg - base
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(p.label, modifier = Modifier.weight(1.6f))
                Text(avg.fmt(metric), modifier = Modifier.weight(1f))
                Text(p.min(metric).fmt(metric), modifier = Modifier.weight(1f))
                Text(p.max(metric).fmt(metric), modifier = Modifier.weight(1f))
                Text(
                    diff?.let { (if (it > 0) "+" else "") + "%.1f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun Double?.fmt(metric: Metric): String =
    this?.let { "%.1f %s".format(it, metric.unit) } ?: "—"
