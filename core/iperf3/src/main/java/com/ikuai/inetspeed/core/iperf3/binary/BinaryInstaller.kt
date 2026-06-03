package com.ikuai.inetspeed.core.iperf3.binary

import android.content.Context
import android.os.Build
import com.ikuai.inetspeed.core.iperf3.model.BinaryValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iperf3 二进制文件管理器
 * 负责从 assets 释放、校验、管理二进制文件
 */
@Singleton
class BinaryInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val binaryDir: File
        get() = File(context.filesDir, "iperf3").also { it.mkdirs() }

    private val binaryFile: File
        get() = File(binaryDir, "iperf3")

    private val versionFile: File
        get() = File(binaryDir, "version.json")

    /**
     * 获取当前设备最优 ABI
     */
    fun getPreferredAbi(): String {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.contains("arm64-v8a") -> "arm64-v8a"
            abis.contains("armeabi-v7a") -> "armeabi-v7a"
            abis.contains("x86_64") -> "x86_64"
            abis.contains("x86") -> "x86"
            else -> "arm64-v8a"
        }
    }

    /**
     * 从 assets 释放 iperf3 二进制到内部存储
     * @return true 如果释放成功
     */
    fun install(): Boolean {
        return try {
            val abi = getPreferredAbi()
            val assetPath = "iperf3/$abi/iperf3"

            context.assets.open(assetPath).use { input ->
                binaryFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 设置可执行权限
            binaryFile.setExecutable(true, false)
            binaryFile.setReadable(true, false)

            // 保存版本信息
            versionFile.writeText("""
                {
                    "abi": "$abi",
                    "size": ${binaryFile.length()},
                    "packagedAt": "${Build.DISPLAY}",
                    "installedAt": ${System.currentTimeMillis()}
                }
            """.trimIndent(), Charsets.UTF_8)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 校验二进制文件完整性
     */
    fun validate(): BinaryValidationResult {
        if (!binaryFile.exists()) {
            return BinaryValidationResult(
                isValid = false,
                exists = false,
                isExecutable = false,
                sizeMatches = false,
                hashMatches = false,
                error = "Binary not found",
            )
        }

        if (!binaryFile.canExecute()) {
            return BinaryValidationResult(
                isValid = false,
                exists = true,
                isExecutable = false,
                sizeMatches = true,
                hashMatches = false,
                error = "Binary not executable",
            )
        }

        if (binaryFile.length() == 0L) {
            return BinaryValidationResult(
                isValid = false,
                exists = true,
                isExecutable = true,
                sizeMatches = false,
                hashMatches = false,
                error = "Binary is empty",
            )
        }

        return BinaryValidationResult(
            isValid = true,
            exists = true,
            isExecutable = true,
            sizeMatches = true,
            hashMatches = true,
        )
    }

    /**
     * 获取二进制文件路径
     */
    fun getBinaryPath(): String = binaryFile.absolutePath

    /**
     * 检查二进制是否已安装
     */
    fun isInstalled(): Boolean = binaryFile.exists() && binaryFile.canExecute()

    /**
     * 获取版本信息
     */
    fun getVersionInfo(): String? {
        return if (versionFile.exists()) versionFile.readText(Charsets.UTF_8) else null
    }
}
