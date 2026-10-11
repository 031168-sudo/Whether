package ru.andrey.whether.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.andrey.whether.data.DayRecord
import ru.andrey.whether.data.Metric

/** Карточка с заголовком: группирует связанные элементы экрана. */
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

/** Все показатели в одну прокручиваемую строку. */
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
                label = { Text(m.label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

/** Значение показателя с единицей измерения, либо прочерк. */
fun Metric.format(value: Double?): String =
    value?.let { "%.1f %s".format(it, unit) } ?: "—"

/** Таблица по дням: дата, минимум, среднее, максимум, осадки. */
@Composable
fun DailyTable(records: List<DayRecord>) {
    val header = MaterialTheme.typography.labelSmall
    val body = MaterialTheme.typography.bodySmall
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Дата", modifier = Modifier.weight(1.3f), style = header)
            Text("Мин", modifier = Modifier.weight(1f), style = header)
            Text("Сред", modifier = Modifier.weight(1f), style = header)
            Text("Макс", modifier = Modifier.weight(1f), style = header)
            Text("Осадки", modifier = Modifier.weight(1f), style = header)
        }
        records.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(r.date, modifier = Modifier.weight(1.3f), style = body)
                Text(r.tMin.fmtNum(), modifier = Modifier.weight(1f), style = body)
                Text(r.tMean.fmtNum(), modifier = Modifier.weight(1f), style = body)
                Text(r.tMax.fmtNum(), modifier = Modifier.weight(1f), style = body)
                Text(r.precipitation.fmtNum(), modifier = Modifier.weight(1f), style = body)
            }
        }
    }
}

private fun Double?.fmtNum(): String = this?.let { "%.1f".format(it) } ?: "—"
