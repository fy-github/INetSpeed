package com.ikuai.inetspeed.feature.speedtest.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 仪表盘组件 - 弧形背景 + 指针 + 速度数字
 */
@Composable
fun GaugeCanvas(
    speedMbps: Double,
    maxSpeedMbps: Double = 1000.0,
    modifier: Modifier = Modifier,
) {
    // 归一化速度值到 0-180 度范围
    val normalizedSpeed = (speedMbps / maxSpeedMbps).coerceIn(0.0, 1.0)
    val targetAngle = (normalizedSpeed * 180f).toFloat()

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 500),
        label = "needle_angle",
    )

    val gaugeBg = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val gaugeFg = MaterialTheme.colorScheme.primary
    val needleColor = MaterialTheme.colorScheme.onBackground
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .height(130.dp),
            ) {
                val strokeWidth = 10.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height * 2 - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                // 背景弧
                drawArc(
                    color = gaugeBg,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                // 前景弧（根据速度填充）
                drawArc(
                    color = gaugeFg,
                    startAngle = 180f,
                    sweepAngle = animatedAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                // 指针
                val centerX = size.width / 2
                val centerY = size.height - strokeWidth / 2
                val needleLength = size.height * 0.65f
                val angleRad = Math.toRadians((180 + animatedAngle).toDouble())
                val needleEndX = centerX + needleLength * cos(angleRad).toFloat()
                val needleEndY = centerY + needleLength * sin(angleRad).toFloat()

                drawLine(
                    color = needleColor,
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // 指针中心圆点
                drawCircle(
                    color = needleColor,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, centerY),
                )

                // 刻度标签
                val labelRadius = size.height * 0.85f
                val labels = listOf("0", "250", "500", "750", "1G")
                labels.forEachIndexed { index, label ->
                    val labelAngle = Math.toRadians((180 + index * 45f).toDouble())
                    val labelX = centerX + labelRadius * cos(labelAngle).toFloat()
                    val labelY = centerY + labelRadius * sin(labelAngle).toFloat()

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(label, labelX, labelY, paint)
                    }
                }
            }

            // 速度数字
            Text(
                text = formatSpeed(speedMbps),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
            )
            Text(
                text = "Mbps",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatSpeed(mbps: Double): String {
    return when {
        mbps >= 1000 -> String.format("%.1f", mbps / 1000)
        mbps >= 100 -> String.format("%.0f", mbps)
        mbps >= 10 -> String.format("%.1f", mbps)
        else -> String.format("%.2f", mbps)
    }
}
