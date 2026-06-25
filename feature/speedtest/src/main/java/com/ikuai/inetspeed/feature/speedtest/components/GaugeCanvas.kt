package com.ikuai.inetspeed.feature.speedtest.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
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
    val accent = MaterialTheme.colorScheme.secondary
    val needleColor = MaterialTheme.colorScheme.onBackground
    val textColor = MaterialTheme.colorScheme.onBackground
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f * 2.75f // 约 10sp
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f),
            ) {
                val strokeWidth = 10.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height * 2 - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val centerX = size.width / 2
                val centerY = size.height - strokeWidth / 2

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

                // 细微强调弧，增强拟物层次
                drawArc(
                    color = accent.copy(alpha = 0.16f),
                    startAngle = 180f,
                    sweepAngle = maxOf(animatedAngle - 12f, 0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round),
                )

                // 指针
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

                // 刻度短线
                val tickRadius = size.height * 0.77f
                listOf(0f, 45f, 90f, 135f, 180f).forEach { tickAngle ->
                    val rad = Math.toRadians((180 + tickAngle).toDouble())
                    val start = Offset(
                        centerX + (tickRadius - 6.dp.toPx()) * cos(rad).toFloat(),
                        centerY + (tickRadius - 6.dp.toPx()) * sin(rad).toFloat(),
                    )
                    val end = Offset(
                        centerX + tickRadius * cos(rad).toFloat(),
                        centerY + tickRadius * sin(rad).toFloat(),
                    )
                    drawLine(
                        color = tickColor,
                        start = start,
                        end = end,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // 刻度标签
                val labelRadius = size.height * 0.85f
                val labels = listOf("0", "250", "500", "750", "1G")
                labels.forEachIndexed { index, label ->
                    val labelAngle = Math.toRadians((180 + index * 45f).toDouble())
                    val labelX = centerX + labelRadius * cos(labelAngle).toFloat()
                    val labelY = centerY + labelRadius * sin(labelAngle).toFloat()

                    drawContext.canvas.nativeCanvas.drawText(label, labelX, labelY, labelPaint)
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
