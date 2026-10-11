package ru.andrey.whether.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import ru.andrey.whether.data.DayRecord
import ru.andrey.whether.data.WeatherRepository
import ru.andrey.whether.location.DeviceLocation

@Composable
fun HistoryScreen(repository: WeatherRepository = WeatherRepository()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var latText by remember { mutableStateOf("55.75") }
    var lonText by remember { mutableStateOf("37.62") }
    var startText by remember { mutableStateOf("2025-11-01") }
    var endText by remember { mutableStateOf("2025-11-30") }
    var records by remember { mutableStateOf<List<DayRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        val lat = latText.toDoubleOrNull()
        val lon = lonText.toDoubleOrNull()
        if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            error = "Проверьте широту (-90..90) и долготу (-180..180)"
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                records = repository.history(lat, lon, startText, endText)
                if (records.isEmpty()) error = "Нет данных за выбранный период"
            } catch (e: Exception) {
                error = "Не удалось загрузить данные: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            val coords = DeviceLocation.lastKnown(context)
            if (coords != null) {
                latText = "%.4f".format(coords.latitude)
                lonText = "%.4f".format(coords.longitude)
            } else {
                error = "Не удалось определить местоположение, введите координаты вручную"
            }
        } else {
            error = "Нет разрешения на геолокацию, введите координаты вручную"
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Широта") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = lonText,
                onValueChange = { lonText = it },
                label = { Text("Долгота") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Button(onClick = {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val coords = DeviceLocation.lastKnown(context)
                if (coords != null) {
                    latText = "%.4f".format(coords.latitude)
                    lonText = "%.4f".format(coords.longitude)
                }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }) {
            Text("Использовать моё местоположение")
        }

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
            TemperatureChart(records)
            DailyTable(records)
        }
    }
}

@Composable
private fun TemperatureChart(records: List<DayRecord>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val meanColor = MaterialTheme.colorScheme.tertiary
    val values = records.flatMap { listOfNotNull(it.tMax, it.tMin) }
    if (values.isEmpty()) return
    val minV = values.min()
    val maxV = values.max()
    val range = (maxV - minV).takeIf { it > 0 } ?: 1.0

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
    ) {
        fun yFor(v: Double) = size.height - ((v - minV) / range * size.height).toFloat()
        fun xFor(i: Int) = if (records.size == 1) 0f else i * size.width / (records.size - 1)

        fun drawSeries(pick: (DayRecord) -> Double?, color: Color) {
            val path = Path()
            var started = false
            records.forEachIndexed { i, r ->
                val v = pick(r) ?: return@forEachIndexed
                if (!started) {
                    path.moveTo(xFor(i), yFor(v)); started = true
                } else {
                    path.lineTo(xFor(i), yFor(v))
                }
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }

        drawSeries({ it.tMax }, lineColor)
        drawSeries({ it.tMin }, meanColor)
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
