package com.ikuai.inetspeed.feature.history.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 趋势柱状图 - 自绘，无外部依赖
 */
@Composable
fun TrendChart(
    data: List<ChartBar>,
    modifier: Modifier = Modifier,
    maxValue: Double = data.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val textColor = android.graphics.Color.GRAY

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        val barCount = data.size
        if (barCount == 0) return@Canvas

        val barWidth = (size.width - (barCount + 1) * 8.dp.toPx()) / barCount
        val chartHeight = size.height - 30.dp.toPx()

        data.forEachIndexed { index, bar ->
            val barHeight = ((bar.value / maxValue) * chartHeight).toFloat()
            val x = 8.dp.toPx() + index * (barWidth + 8.dp.toPx())
            val y = chartHeight - barHeight + 10.dp.toPx()

            // 柱子
            drawRoundRect(
                color = bar.color ?: barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )

            // 标签
            drawContext.canvas.nativeCanvas.drawText(
                bar.label,
                x + barWidth / 2,
                size.height - 2.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                },
            )

            // 值
            if (barHeight > 20.dp.toPx()) {
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.0f", bar.value),
                    x + barWidth / 2,
                    y - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    },
                )
            }
        }
    }
}

data class ChartBar(
    val label: String,
    val value: Double,
    val color: Color? = null,
)
