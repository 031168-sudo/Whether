package ru.andrey.whether.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Линейный график с сеткой, подписями значений по оси Y и подписями начала и конца по оси X.
 * Каждая серия — пара (цвет, значения по дням). Пропуски (null) не рисуются.
 */
@Composable
fun LineChart(
    series: List<Pair<Color, List<Double?>>>,
    xStartLabel: String,
    xEndLabel: String,
    modifier: Modifier = Modifier.fillMaxWidth().height(240.dp)
) {
    val all = series.flatMap { it.second }.filterNotNull()
    if (all.isEmpty()) {
        Text("Нет данных для графика")
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = remember(labelColor) { TextStyle(fontSize = 11.sp, color = labelColor) }
    val endLabelWidth = textMeasurer.measure(xEndLabel, labelStyle).size.width.toFloat()

    val minV = all.min()
    val maxV = all.max()
    val pad = ((maxV - minV).takeIf { it > 0 } ?: 1.0) * 0.1
    val lo = minV - pad
    val hi = maxV + pad
    val length = series.maxOf { it.second.size }

    Canvas(modifier = modifier) {
        val left = 48f
        val top = 8f
        val bottom = size.height - 22f
        val plotW = size.width - left - 8f
        val plotH = bottom - top

        fun xFor(i: Int) = if (length <= 1) left else left + i * plotW / (length - 1)
        fun yFor(v: Double) = top + plotH - ((v - lo) / (hi - lo) * plotH).toFloat()

        // Горизонтальная сетка с подписями значений: 5 уровней.
        for (k in 0..4) {
            val value = lo + (hi - lo) * k / 4
            val y = yFor(value)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1f)
            drawText(
                textMeasurer,
                "%.1f".format(value),
                topLeft = Offset(0f, y - 8f),
                style = labelStyle
            )
        }

        // Подписи по оси X: начало и конец.
        drawText(textMeasurer, xStartLabel, topLeft = Offset(left, bottom + 4f), style = labelStyle)
        drawText(
            textMeasurer,
            xEndLabel,
            topLeft = Offset(size.width - endLabelWidth, bottom + 4f),
            style = labelStyle
        )

        series.forEach { (color, values) ->
            val path = Path()
            var started = false
            values.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                if (!started) {
                    path.moveTo(xFor(i), yFor(v)); started = true
                } else {
                    path.lineTo(xFor(i), yFor(v))
                }
            }
            drawPath(path, color, style = Stroke(width = 4f))
        }
    }
}
