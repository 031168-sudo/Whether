package ru.andrey.whether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.andrey.whether.data.OpenMeteoArchiveApi
import ru.andrey.whether.data.WeatherRepository
import ru.andrey.whether.data.db.AppDatabase
import ru.andrey.whether.ui.ComparisonScreen
import ru.andrey.whether.ui.CoordinatesState
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
                    val coords = remember { CoordinatesState() }
                    var tab by remember { mutableStateOf(0) }

                    Column {
                        TabRow(selectedTabIndex = tab) {
                            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                                Text("История", modifier = Modifier.padding(16.dp))
                            }
                            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                                Text("Сравнение", modifier = Modifier.padding(16.dp))
                            }
                        }
                        when (tab) {
                            0 -> HistoryScreen(repository, coords)
                            else -> ComparisonScreen(repository, coords)
                        }
                    }
                }
            }
        }
    }
}
