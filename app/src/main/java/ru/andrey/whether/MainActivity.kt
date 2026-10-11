package ru.andrey.whether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.andrey.whether.data.OpenMeteoArchiveApi
import ru.andrey.whether.data.WeatherRepository
import ru.andrey.whether.data.db.AppDatabase
import ru.andrey.whether.ui.ComparisonScreen
import ru.andrey.whether.ui.CoordinatesState
import ru.andrey.whether.ui.ForecastScreen
import ru.andrey.whether.ui.HistoryScreen
import ru.andrey.whether.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Один репозиторий и одно состояние координат на все экраны.
                    val repository = remember {
                        WeatherRepository(
                            api = OpenMeteoArchiveApi.create(),
                            dao = AppDatabase.create(this@MainActivity).daily()
                        )
                    }
                    val coords = remember { CoordinatesState.create(this@MainActivity) }
                    var tab by remember { mutableStateOf(0) }
                    var showSettings by remember { mutableStateOf(false) }
                    val titles = listOf("Прогноз", "История", "Сравнение")

                    // safeDrawing отступает от статус-бара сверху и от навигации снизу.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        TopBar(
                            showSettings = showSettings,
                            coordsSummary = coords.summary(),
                            onOpenSettings = { showSettings = true },
                            onBack = { showSettings = false }
                        )
                        if (!showSettings) {
                            TabRow(selectedTabIndex = tab) {
                                titles.forEachIndexed { i, title ->
                                    Tab(selected = tab == i, onClick = { tab = i }) {
                                        Text(
                                            title,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            when {
                                showSettings -> SettingsScreen(repository, coords)
                                tab == 0 -> ForecastScreen(repository, coords)
                                tab == 1 -> HistoryScreen(repository, coords)
                                else -> ComparisonScreen(repository, coords)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    showSettings: Boolean,
    coordsSummary: String,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSettings) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Настройки", style = MaterialTheme.typography.titleLarge)
        } else {
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Whether", style = MaterialTheme.typography.titleLarge)
                Text(
                    coordsSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Настройки")
            }
        }
    }
}
