package com.ikuai.inetspeed.core.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val exportDir: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "INetSpeed").also { it.mkdirs() }

    /**
     * 导出 CSV
     */
    fun exportCsv(measurements: List<TestMeasurement>, title: String): File {
        val file = File(exportDir, "${sanitizeFilename(title)}.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("时间,服务器,协议,方向,下行(Mbps),上行(Mbps),延迟(ms),抖动(ms),丢包(%)")
            measurements.forEach { m ->
                writer.appendLine(
                    "${formatDate(m.timestamp)},${m.serverName},${m.protocol},${m.direction}," +
                    "${m.throughputMbps},${m.uploadMbps ?: ""},${m.latencyMs ?: ""}," +
                    "${m.jitterMs ?: ""},${m.packetLossPercent ?: ""}"
                )
            }
        }
        return file
    }

    /**
     * 导出 PDF
     */
    fun exportPdf(measurements: List<TestMeasurement>, title: String): File {
        val file = File(exportDir, "${sanitizeFilename(title)}.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.DKGRAY
        }
        val headerPaint = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }

        var y = 40f

        // 标题
        canvas.drawText(title, 40f, y, titlePaint)
        y += 30f
        canvas.drawText("生成时间: ${formatDate(System.currentTimeMillis())}", 40f, y, bodyPaint)
        y += 30f

        // 表头
        val headers = listOf("时间", "服务器", "协议", "下行", "延迟")
        val colWidths = listOf(120f, 100f, 50f, 80f, 60f)
        var x = 40f
        headers.forEachIndexed { i, header ->
            canvas.drawText(header, x, y, headerPaint)
            x += colWidths[i]
        }
        y += 18f

        // 数据行
        measurements.take(40).forEach { m ->
            x = 40f
            canvas.drawText(formatDate(m.timestamp), x, y, bodyPaint); x += colWidths[0]
            canvas.drawText(m.serverName.take(12), x, y, bodyPaint); x += colWidths[1]
            canvas.drawText(m.protocol.uppercase(), x, y, bodyPaint); x += colWidths[2]
            canvas.drawText(String.format("%.1f", m.throughputMbps), x, y, bodyPaint); x += colWidths[3]
            canvas.drawText(m.latencyMs?.let { "${it.toInt()}" } ?: "-", x, y, bodyPaint)
            y += 16f

            if (y > 800f) {
                document.finishPage(page)
                val newPage = document.startPage(pageInfo)
                y = 40f
            }
        }

        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_").take(50)
    }
}
