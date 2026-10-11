package ru.andrey.whether.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ru.andrey.whether.location.Coordinates
import ru.andrey.whether.location.DeviceLocation
import java.util.Locale

/**
 * Общее состояние координат для всех вкладок. Создаётся один раз в MainActivity,
 * поэтому ввод на одной вкладке виден на другой.
 */
class CoordinatesState {
    var latText by mutableStateOf("55.75")
    var lonText by mutableStateOf("37.62")

    /** Принимает и точку, и запятую как разделитель. Возвращает null, если координаты некорректны. */
    fun parse(): Coordinates? {
        val lat = latText.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val lon = lonText.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return Coordinates(lat, lon)
    }
}

/** Поля широты и долготы с кнопкой «моё местоположение». Сообщения об ошибках уходят в onMessage. */
@Composable
fun CoordinatesInput(state: CoordinatesState, onMessage: (String?) -> Unit) {
    val context = LocalContext.current

    fun applyDevice() {
        val c = DeviceLocation.lastKnown(context)
        if (c != null) {
            state.latText = String.format(Locale.US, "%.4f", c.latitude)
            state.lonText = String.format(Locale.US, "%.4f", c.longitude)
            onMessage(null)
        } else {
            onMessage("Не удалось определить местоположение, введите координаты вручную")
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) applyDevice()
        else onMessage("Нет разрешения на геолокацию, введите координаты вручную")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.latText,
                onValueChange = { state.latText = it },
                label = { Text("Широта") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.lonText,
                onValueChange = { state.lonText = it },
                label = { Text("Долгота") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(onClick = {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) applyDevice()
            else launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }) {
            Text("Использовать моё местоположение")
        }
    }
}
