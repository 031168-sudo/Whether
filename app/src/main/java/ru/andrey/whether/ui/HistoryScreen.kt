package ru.andrey.whether.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            error = "Проверьте координаты в настройках"
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Section("Период") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactField(startText, { startText = it }, "С (ГГГГ-ММ-ДД)", Modifier.weight(1f))
                CompactField(endText, { endText = it }, "По (ГГГГ-ММ-ДД)", Modifier.weight(1f))
            }
            Button(onClick = { load() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Показать историю", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (records.isNotEmpty()) {
            Section("Показатель") { MetricChips(metric) { metric = it } }
            val values = records.map(metric.pick)
            Section(metric.label) {
                LineChart(
                    series = listOf(MaterialTheme.colorScheme.primary to values),
                    xStartLabel = records.first().date,
                    xEndLabel = records.last().date,
                    describe = { i ->
                        listOf(records[i].date, "${metric.label}: ${metric.format(values[i])}")
                    }
                )
                Text(
                    "Нажмите или проведите по графику, чтобы увидеть день",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Section("По дням") { DailyTable(records) }
        }
    }
}
