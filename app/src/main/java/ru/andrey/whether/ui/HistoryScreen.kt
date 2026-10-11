package ru.andrey.whether.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.andrey.whether.data.DayRecord
import ru.andrey.whether.data.Metric
import ru.andrey.whether.data.WeatherRepository

@Composable
fun HistoryScreen(repository: WeatherRepository, coords: CoordinatesState) {
    val scope = rememberCoroutineScope()

    var startText by remember { mutableStateOf("2025-11-01") }
    var endText by remember { mutableStateOf("2025-11-30") }
    var metric by remember { mutableStateOf(Metric.T_MEAN) }
    var records by remember { mutableStateOf<List<DayRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        val c = coords.parse()
        if (c == null) {
            error = "Проверьте широту (-90..90) и долготу (-180..180)"
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                records = repository.history(c.latitude, c.longitude, startText, endText)
                if (records.isEmpty()) error = "Нет данных за выбранный период"
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
        Text("История погоды", style = MaterialTheme.typography.headlineSmall)

        CoordinatesInput(coords) { error = it }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it },
                label = { Text("С (ГГГГ-ММ-ДД)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it },
                label = { Text("По (ГГГГ-ММ-ДД)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Button(onClick = { load() }, enabled = !loading) {
            Text("Показать историю")
        }

        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (records.isNotEmpty()) {
            Text("Показатель", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Metric.entries.take(3).forEach { m ->
                    FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Metric.entries.drop(3).forEach { m ->
                    FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) })
                }
            }
            MetricChart(records, metric)
            DailyTable(records)
        }
    }
}

/** График одной метрики по дням с подписями значений и дат. */
@Composable
private fun MetricChart(records: List<DayRecord>, metric: Metric) {
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)

    // Точки в виде пар (индекс дня, значение); пропуски не рисуются.
    val points = records.mapIndexedNotNull { i, r -> metric.pick(r)?.let { i to it } }
    if (points.isEmpty()) {
        Text("Для этого показателя нет данных")
        return
    }
    val minV = points.minOf { it.second }
    val maxV = points.maxOf { it.second }
    val range = (maxV - minV).takeIf { it > 0 } ?: 1.0
    val first = records.first().date
    val last = records.last().date

    Text("${metric.label}, ${metric.unit}", style = MaterialTheme.typography.labelLarge)

    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val left = 40f          // место под подписи по оси Y
        val bottom = size.height - 20f  // место под даты
        val plotW = size.width - left
        val plotH = bottom

        fun xFor(i: Int) = if (records.size <= 1) left else left + i * plotW / (records.size - 1)
        fun yFor(v: Double) = plotH - ((v - minV) / range * plotH).toFloat()

        val path = Path()
        var started = false
        points.forEach { (i, v) ->
            if (!started) {
                path.moveTo(xFor(i), yFor(v)); started = true
            } else {
                path.lineTo(xFor(i), yFor(v))
            }
        }
        drawPath(path, lineColor, style = Stroke(width = 4f))

        // Подписи по оси Y: максимум и минимум.
        drawText(textMeasurer, "%.0f".format(maxV), topLeft = Offset(0f, 0f), style = labelStyle)
        drawText(textMeasurer, "%.0f".format(minV), topLeft = Offset(0f, bottom - 14f), style = labelStyle)
        // Даты по оси X: первый и последний день.
        drawText(textMeasurer, first, topLeft = Offset(left, bottom + 4f), style = labelStyle)
        drawText(textMeasurer, last, topLeft = Offset(size.width - 90f, bottom + 4f), style = labelStyle)
    }
}

@Composable
private fun DailyTable(records: List<DayRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Дата", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium)
            Text("Min", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Ср", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Max", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Осадки", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        }
        records.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(r.date, modifier = Modifier.weight(1.4f))
                Text(r.tMin.fmt(), modifier = Modifier.weight(1f))
                Text(r.tMean.fmt(), modifier = Modifier.weight(1f))
                Text(r.tMax.fmt(), modifier = Modifier.weight(1f))
                Text(r.precipitation.fmt(), modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun Double?.fmt(): String = this?.let { "%.1f".format(it) } ?: "—"
