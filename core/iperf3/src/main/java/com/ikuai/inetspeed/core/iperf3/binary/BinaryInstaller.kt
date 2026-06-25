package com.ikuai.inetspeed.core.iperf3.binary

import android.content.Context
import android.os.Build
import com.ikuai.inetspeed.core.iperf3.model.BinaryValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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

    fun getPreferredAbi(): String {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.contains("x86_64") -> "x86_64"
            abis.contains("arm64-v8a") -> "arm64-v8a"
            abis.contains("x86") -> "x86"
            abis.contains("armeabi-v7a") -> "armeabi-v7a"
            else -> "arm64-v8a"
        }
    }

    fun install(): Boolean {
        return try {
            val abi = getPreferredAbi()
            val assetPath = "iperf3/$abi/iperf3"

            context.assets.open(assetPath).use { input ->
                binaryFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            binaryFile.setExecutable(true, false)
            binaryFile.setReadable(true, false)

            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath)).waitFor()
            } catch (e: Exception) {
                android.util.Log.w("BinaryInstaller", "Failed to chmod binary", e)
            }

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

    fun validate(): BinaryValidationResult {
        if (!binaryFile.exists() && !nativeBinaryExists()) {
            return BinaryValidationResult(
                isValid = false, exists = false, isExecutable = false,
                sizeMatches = false, hashMatches = false, error = "Binary not found",
            )
        }

        if (!binaryFile.canExecute() && !nativeBinaryExists()) {
            return BinaryValidationResult(
                isValid = false, exists = true, isExecutable = false,
                sizeMatches = true, hashMatches = false, error = "Binary not executable",
            )
        }

        if (binaryFile.exists() && binaryFile.length() == 0L && !nativeBinaryExists()) {
            return BinaryValidationResult(
                isValid = false, exists = true, isExecutable = true,
                sizeMatches = false, hashMatches = false, error = "Binary is empty",
            )
        }

        return BinaryValidationResult(
            isValid = true, exists = true, isExecutable = true,
            sizeMatches = true, hashMatches = true,
        )
    }

    /**
     * 优先使用 nativeLibraryDir 中的 libiperf3.so（SELinux 允许执行），
     * 回退到 filesDir 中的 iperf3。
     */
    fun getBinaryPath(): String {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        if (nativeDir != null) {
            val nativeFile = File(nativeDir, "libiperf3.so")
            if (nativeFile.exists() && nativeFile.canExecute()) {
                return nativeFile.absolutePath
            }
        }
        return binaryFile.absolutePath
    }

    fun isInstalled(): Boolean = binaryFile.exists() || nativeBinaryExists()

    fun getVersionInfo(): String? {
        return if (versionFile.exists()) versionFile.readText(Charsets.UTF_8) else null
    }

    private fun nativeBinaryExists(): Boolean {
        val nativeDir = context.applicationInfo.nativeLibraryDir ?: return false
        return File(nativeDir, "libiperf3.so").exists()
    }
}
