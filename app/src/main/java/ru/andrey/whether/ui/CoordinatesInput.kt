package ru.andrey.whether.ui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ru.andrey.whether.location.Coordinates
import ru.andrey.whether.location.DeviceLocation
import java.util.Locale

/**
 * Общее состояние координат. Сохраняется в SharedPreferences и восстанавливается при запуске.
 */
class CoordinatesState(private val prefs: SharedPreferences) {
    var latText by mutableStateOf(prefs.getString(KEY_LAT, DEFAULT_LAT) ?: DEFAULT_LAT)
    var lonText by mutableStateOf(prefs.getString(KEY_LON, DEFAULT_LON) ?: DEFAULT_LON)

    /**
     * Принимает и точку, и запятую как разделитель. Возвращает null, если координаты некорректны.
     * Корректные координаты сохраняются.
     */
    fun parse(): Coordinates? {
        val lat = latText.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val lon = lonText.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        prefs.edit().putString(KEY_LAT, latText).putString(KEY_LON, lonText).apply()
        return Coordinates(lat, lon)
    }

    /** Короткая подпись для шапки. */
    fun summary(): String = "${latText.trim()}, ${lonText.trim()}"

    companion object {
        private const val PREFS_NAME = "whether_prefs"
        private const val KEY_LAT = "last_lat"
        private const val KEY_LON = "last_lon"
        private const val DEFAULT_LAT = "55.75"
        private const val DEFAULT_LON = "37.62"

        fun create(context: Context): CoordinatesState =
            CoordinatesState(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}

/** Поля широты и долготы и кнопка определения по устройству. Сообщения уходят в onMessage. */
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
            onMessage("Не удалось определить местоположение. Введите координаты вручную.")
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) applyDevice()
        else onMessage("Нет разрешения на геолокацию. Введите координаты вручную.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactField(
                value = state.latText,
                onValueChange = { state.latText = it },
                label = "Широта",
                modifier = Modifier.weight(1f)
            )
            CompactField(
                value = state.lonText,
                onValueChange = { state.lonText = it },
                label = "Долгота",
                modifier = Modifier.weight(1f)
            )
        }
        FilledTonalButton(
            onClick = {
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Определить по устройству", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Компактное поле ввода: мелкий текст и десятичная клавиатура. */
@Composable
fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
