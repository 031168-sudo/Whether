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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            MetricChips(metric) { metric = it }
            Text("${metric.label}, ${metric.unit}", style = MaterialTheme.typography.labelLarge)
            LineChart(
                series = listOf(MaterialTheme.colorScheme.primary to records.map(metric.pick)),
                xStartLabel = records.first().date,
                xEndLabel = records.last().date
            )
            DailyTable(records)
        }
    }
}
