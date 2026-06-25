package com.ikuai.inetspeed.core.data.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * raw output 文件管理器
 * 管理 iperf3/Ping/Traceroute 的原始输出文件
 */
@Singleton
class RawOutputManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val outputDir: File
        get() = File(context.filesDir, "iperf3-outputs").also { it.mkdirs() }

    /**
     * 保存 raw output 到文件
     * @return 文件路径
     */
    fun save(testId: String, content: String): String {
        val file = File(outputDir, "${testId}_${System.currentTimeMillis()}.txt")
        file.writeText(content, Charsets.UTF_8)
        return file.absolutePath
    }

    /**
     * 读取 raw output 文件内容
     */
    fun read(path: String): String? {
        val file = File(path)
        if (!isWithinOutputDir(file)) return null
        return if (file.exists() && file.canRead()) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    /**
     * 删除 raw output 文件
     */
    fun delete(path: String): Boolean {
        val file = File(path)
        if (!isWithinOutputDir(file)) return false
        return if (file.exists()) file.delete() else true
    }

    /**
     * 清理超过指定天数的文件
     */
    fun cleanup(olderThanDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - olderThanDays * 24 * 60 * 60 * 1000L
        outputDir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    /**
     * 获取文件大小
     */
    fun getSize(path: String): Long {
        val file = File(path)
        if (!isWithinOutputDir(file)) return 0
        return if (file.exists()) file.length() else 0
    }

    private fun isWithinOutputDir(file: File): Boolean {
        return file.canonicalPath.startsWith(outputDir.canonicalPath)
    }
}
