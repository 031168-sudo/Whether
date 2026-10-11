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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Линейный график.
 *
 * - Ноль всегда в диапазоне и показан линией через весь график.
 * - Нажатие или протягивание по горизонтали выбирает день: маркеры и подсказка из [describe].
 * - Серия — пара (цвет, значения по дням); пропуски (null) не рисуются.
 */
@Composable
fun LineChart(
    series: List<Pair<Color, List<Double?>>>,
    xStartLabel: String,
    xEndLabel: String,
    describe: (Int) -> List<String>,
    modifier: Modifier = Modifier.fillMaxWidth().height(280.dp)
) {
    val all = series.flatMap { it.second }.filterNotNull()
    if (all.isEmpty()) {
        Text("Нет данных для графика")
        return
    }

    val density = LocalDensity.current
    // Все отступы считаются в dp и переводятся в пиксели, иначе на разных экранах подписи обрезаются.
    val leftPx = with(density) { 44.dp.toPx() }
    val rightPx = with(density) { 12.dp.toPx() }
    val topPx = with(density) { 12.dp.toPx() }
    val bottomPx = with(density) { 30.dp.toPx() }

    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val zeroColor = MaterialTheme.colorScheme.onSurface
    val tipBg = MaterialTheme.colorScheme.surface
    val tipText = MaterialTheme.colorScheme.onSurface
    val labelStyle = remember(labelColor) { TextStyle(fontSize = 11.sp, color = labelColor) }
    val tipStyle = remember(tipText) { TextStyle(fontSize = 12.sp, color = tipText) }
    val endLabelWidth = textMeasurer.measure(xEndLabel, labelStyle).size.width.toFloat()

    // Диапазон всегда включает ноль, поэтому линия нуля видна.
    val minV = min(all.min(), 0.0)
    val maxV = max(all.max(), 0.0)
    val pad = ((maxV - minV).takeIf { it > 0 } ?: 1.0) * 0.1
    val lo = minV - pad
    val hi = maxV + pad
    val length = series.maxOf { it.second.size }

    var selected by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier.pointerInput(length) {
            fun indexAt(x: Float): Int {
                if (length <= 1) return 0
                val plotW = size.width - leftPx - rightPx
                return ((x - leftPx) / plotW * (length - 1)).roundToInt().coerceIn(0, length - 1)
            }
            detectTapGestures(onTap = { selected = indexAt(it.x) })
            detectHorizontalDragGestures(
                onDragStart = { selected = indexAt(it.x) },
                onHorizontalDrag = { change, _ -> selected = indexAt(change.position.x) }
            )
        }
    ) {
        val top = topPx
        val bottom = size.height - bottomPx
        val plotW = size.width - leftPx - rightPx
        val plotH = bottom - top

        fun xFor(i: Int) = if (length <= 1) leftPx else leftPx + i * plotW / (length - 1)
        fun yFor(v: Double) = top + plotH - ((v - lo) / (hi - lo) * plotH).toFloat()

        // Сетка и подписи значений по оси Y.
        for (k in 0..4) {
            val value = lo + (hi - lo) * k / 4
            val y = yFor(value)
            drawLine(gridColor, Offset(leftPx, y), Offset(size.width, y), strokeWidth = 1f)
            drawText(
                textMeasurer,
                "%.0f".format(value),
                topLeft = Offset(0f, y - 8.dp.toPx()),
                style = labelStyle
            )
        }

        // Линия нуля через весь график.
        val zeroY = yFor(0.0)
        drawLine(zeroColor, Offset(leftPx, zeroY), Offset(size.width, zeroY), strokeWidth = 2.dp.toPx())

        // Подписи по оси X: начало и конец под графиком.
        drawText(textMeasurer, xStartLabel, topLeft = Offset(leftPx, bottom + 6.dp.toPx()), style = labelStyle)
        drawText(
            textMeasurer,
            xEndLabel,
            topLeft = Offset(size.width - endLabelWidth, bottom + 6.dp.toPx()),
            style = labelStyle
        )

        // Линии серий.
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
            drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        }

        // Выбранный день: вертикальная линия, маркеры и подсказка.
        selected?.takeIf { it in 0 until length }?.let { i ->
            val x = xFor(i)
            drawLine(labelColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1.5f.dp.toPx())
            series.forEach { (color, values) ->
                values.getOrNull(i)?.let { v ->
                    drawCircle(color, radius = 5.dp.toPx(), center = Offset(x, yFor(v)))
                }
            }
            val text = describe(i).joinToString("\n")
            val layout = textMeasurer.measure(text, tipStyle)
            val boxW = layout.size.width + 16.dp.toPx()
            val boxH = layout.size.height + 12.dp.toPx()
            val boxX = (if (x + 12.dp.toPx() + boxW > size.width) x - 12.dp.toPx() - boxW else x + 12.dp.toPx())
                .coerceAtLeast(0f)
            drawRoundRect(
                color = tipBg,
                topLeft = Offset(boxX, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(12.dp.toPx())
            )
            drawText(
                textMeasurer,
                text,
                topLeft = Offset(boxX + 8.dp.toPx(), top + 6.dp.toPx()),
                style = tipStyle
            )
        }
    }
}
