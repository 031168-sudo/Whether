package ru.andrey.whether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.andrey.whether.data.OpenMeteoArchiveApi
import ru.andrey.whether.data.WeatherRepository
import ru.andrey.whether.data.db.AppDatabase
import ru.andrey.whether.ui.ComparisonScreen
import ru.andrey.whether.ui.CoordinatesState
import ru.andrey.whether.ui.ForecastScreen
import ru.andrey.whether.ui.HistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Один репозиторий и одно состояние координат на все вкладки.
                    val repository = remember {
                        WeatherRepository(
                            api = OpenMeteoArchiveApi.create(),
                            dao = AppDatabase.create(this@MainActivity).daily()
                        )
                    }
                    val coords = remember { CoordinatesState.create(this@MainActivity) }
                    val scope = rememberCoroutineScope()
                    var tab by remember { mutableStateOf(0) }
                    var cacheMessage by remember { mutableStateOf<String?>(null) }
                    val titles = listOf("Прогноз", "История", "Сравнение")

                    // Scaffold отступает от статус-бара сверху и от навигации снизу.
                    Scaffold(
                        topBar = {
                            Column {
                                TabRow(selectedTabIndex = tab) {
                                    titles.forEachIndexed { i, title ->
                                        Tab(selected = tab == i, onClick = { tab = i }) {
                                            Text(title, modifier = Modifier.padding(12.dp))
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            repository.clearCache()
                                            cacheMessage = "Кэш очищен"
                                        }
                                    }) {
                                        Text("Очистить кэш")
                                    }
                                    cacheMessage?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                                }
                            }
                        }
                    ) { inner ->
                        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
                            when (tab) {
                                0 -> ForecastScreen(repository, coords)
                                1 -> HistoryScreen(repository, coords)
                                else -> ComparisonScreen(repository, coords)
                            }
                        }
                    }
                }
            }
        }
    }
}
