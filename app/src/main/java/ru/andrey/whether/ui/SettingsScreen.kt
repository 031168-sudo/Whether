package ru.andrey.whether.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ru.andrey.whether.data.WeatherRepository

@Composable
fun SettingsScreen(repository: WeatherRepository, coords: CoordinatesState) {
    val scope = rememberCoroutineScope()
    var coordsMessage by remember { mutableStateOf<String?>(null) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Section("Координаты") {
            CoordinatesInput(coords) { coordsMessage = it }
            Button(
                onClick = {
                    coordsMessage = if (coords.parse() != null) {
                        "Координаты сохранены"
                    } else {
                        "Проверьте значения: широта от -90 до 90, долгота от -180 до 180"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить", style = MaterialTheme.typography.labelLarge)
            }
            coordsMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Section("Кэш") {
            Text(
                "Дни старше 7 дней сохраняются на телефоне, чтобы не запрашивать их повторно.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        repository.clearCache()
                        cacheMessage = "Кэш очищен"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Очистить кэш", style = MaterialTheme.typography.labelLarge)
            }
            cacheMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
