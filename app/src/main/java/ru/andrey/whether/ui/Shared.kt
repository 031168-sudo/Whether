package ru.andrey.whether.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.andrey.whether.data.DayRecord
import ru.andrey.whether.data.Metric

/** Все показатели в одну прокручиваемую строку, чтобы подписи не переносились. */
@Composable
fun MetricChips(selected: Metric, onSelect: (Metric) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Metric.entries.forEach { m ->
            FilterChip(
                selected = selected == m,
                onClick = { onSelect(m) },
                label = { Text(m.label) }
            )
        }
    }
}

/** Таблица по дням: дата, минимум, среднее, максимум, осадки. */
@Composable
fun DailyTable(records: List<DayRecord>) {
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
                Text(r.tMin.fmtValue(), modifier = Modifier.weight(1f))
                Text(r.tMean.fmtValue(), modifier = Modifier.weight(1f))
                Text(r.tMax.fmtValue(), modifier = Modifier.weight(1f))
                Text(r.precipitation.fmtValue(), modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun Double?.fmtValue(): String = this?.let { "%.1f".format(it) } ?: "—"
