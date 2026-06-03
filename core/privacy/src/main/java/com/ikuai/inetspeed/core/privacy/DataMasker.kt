package com.ikuai.inetspeed.core.privacy

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据脱敏工具类
 * 对 IP、域名、路由跳点等敏感数据进行脱敏处理
 */
@Singleton
class DataMasker @Inject constructor() {

    /**
     * 脱敏 IPv4 地址
     * 192.168.1.100 → 192.168.*.**
     */
    fun maskIpv4(ip: String): String {
        val parts = ip.split(".")
        if (parts.size != 4) return ip
        return "${parts[0]}.${parts[1]}.*.**"
    }

    /**
     * 脱敏 IPv6 地址
     * 2001:db8::1 → 2001:db8::****
     */
    fun maskIpv6(ip: String): String {
        val parts = ip.split(":")
        if (parts.size < 4) return ip
        return parts.take(3).joinToString(":") + ":****"
    }

    /**
     * 脱敏域名
     * hk-iperf.ikuai.com → hk-iperf.***.com
     */
    fun maskDomain(domain: String): String {
        val parts = domain.split(".")
        if (parts.size < 2) return domain
        return parts.first() + ".***." + parts.last()
    }

    /**
     * 脱敏路由跳点列表
     */
    fun maskRouteHops(hops: String): String {
        return hops.lines().map { line ->
            if (line.contains(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                line.replace(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) { maskIpv4(it.value) }
            } else {
                line
            }
        }.joinToString("\n")
    }

    /**
     * 根据隐私设置处理导出文本
     */
    fun applyPrivacySettings(
        text: String,
        includeIp: Boolean,
        includeDomain: Boolean,
        includeRoute: Boolean,
    ): String {
        var result = text
        if (!includeIp) {
            result = result.replace(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) { maskIpv4(it.value) }
            result = result.replace(Regex("[0-9a-fA-F:]{6,39}")) { maskIpv6(it.value) }
        }
        if (!includeDomain) {
            result = result.replace(Regex("[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) { maskDomain(it.value) }
        }
        return result
    }
}
