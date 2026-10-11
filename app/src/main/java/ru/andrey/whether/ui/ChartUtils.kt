package ru.andrey.whether.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val LEFT = 52f
private const val RIGHT_PAD = 10f
private const val TOP = 10f
private const val BOTTOM_PAD = 26f

/**
 * Линейный график. Нажатие или протягивание пальцем по горизонтали выбирает день:
 * показываются маркеры и подсказка из [describe].
 *
 * Каждая серия — пара (цвет, значения по дням); пропуски (null) не рисуются.
 */
@Composable
fun LineChart(
    series: List<Pair<Color, List<Double?>>>,
    xStartLabel: String,
    xEndLabel: String,
    describe: (Int) -> List<String>,
    modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)
) {
    val all = series.flatMap { it.second }.filterNotNull()
    if (all.isEmpty()) {
        Text("Нет данных для графика")
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val tipBg = MaterialTheme.colorScheme.surface
    val tipText = MaterialTheme.colorScheme.onSurface
    val labelStyle = remember(labelColor) { TextStyle(fontSize = 11.sp, color = labelColor) }
    val tipStyle = remember(tipText) { TextStyle(fontSize = 12.sp, color = tipText) }
    val endLabelWidth = textMeasurer.measure(xEndLabel, labelStyle).size.width.toFloat()

    val minV = all.min()
    val maxV = all.max()
    val pad = ((maxV - minV).takeIf { it > 0 } ?: 1.0) * 0.1
    val lo = minV - pad
    val hi = maxV + pad
    val length = series.maxOf { it.second.size }

    var selected by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier.pointerInput(length) {
            fun indexAt(x: Float): Int {
                if (length <= 1) return 0
                val plotW = size.width - LEFT - RIGHT_PAD
                return ((x - LEFT) / plotW * (length - 1)).roundToInt().coerceIn(0, length - 1)
            }
            detectTapGestures(onTap = { selected = indexAt(it.x) })
            detectHorizontalDragGestures(
                onDragStart = { selected = indexAt(it.x) },
                onHorizontalDrag = { change, _ -> selected = indexAt(change.position.x) }
            )
        }
    ) {
        val top = TOP
        val bottom = size.height - BOTTOM_PAD
        val plotW = size.width - LEFT - RIGHT_PAD
        val plotH = bottom - top

        fun xFor(i: Int) = if (length <= 1) LEFT else LEFT + i * plotW / (length - 1)
        fun yFor(v: Double) = top + plotH - ((v - lo) / (hi - lo) * plotH).toFloat()

        // Сетка и подписи значений по оси Y.
        for (k in 0..4) {
            val value = lo + (hi - lo) * k / 4
            val y = yFor(value)
            drawLine(gridColor, Offset(LEFT, y), Offset(size.width, y), strokeWidth = 1f)
            drawText(
                textMeasurer,
                "%.1f".format(value),
                topLeft = Offset(0f, y - 8f),
                style = labelStyle
            )
        }

        // Подписи по оси X: начало и конец.
        drawText(textMeasurer, xStartLabel, topLeft = Offset(LEFT, bottom + 6f), style = labelStyle)
        drawText(
            textMeasurer,
            xEndLabel,
            topLeft = Offset(size.width - endLabelWidth, bottom + 6f),
            style = labelStyle
        )

        // Линии.
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

        // Выбранный день: вертикальная линия, маркеры и подсказка.
        selected?.takeIf { it in 0 until length }?.let { i ->
            val x = xFor(i)
            drawLine(labelColor, Offset(x, top), Offset(x, bottom), strokeWidth = 2f)
            series.forEach { (color, values) ->
                values.getOrNull(i)?.let { v ->
                    drawCircle(color, radius = 8f, center = Offset(x, yFor(v)))
                }
            }
            val text = describe(i).joinToString("\n")
            val layout = textMeasurer.measure(text, tipStyle)
            val boxW = layout.size.width + 16f
            val boxH = layout.size.height + 12f
            val boxX = if (x + 12f + boxW > size.width) x - 12f - boxW else x + 12f
            drawRoundRect(
                color = tipBg,
                topLeft = Offset(boxX.coerceAtLeast(0f), top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(12f)
            )
            drawText(
                textMeasurer,
                text,
                topLeft = Offset(boxX.coerceAtLeast(0f) + 8f, top + 6f),
                style = tipStyle
            )
        }
    }
}
