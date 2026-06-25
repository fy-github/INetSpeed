package com.ikuai.inetspeed.core.network.info

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkInfoService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class NetworkInfo(
        val networkType: String,
        val ipv4: String?,
        val ipv6: String?,
        val gateway: String?,
        val dns: String?,
        val ssid: String?,
        val signalStrength: Int?,
        val linkSpeed: Int?,
        val macAddress: String?,
    )

    suspend fun collect(): NetworkInfo = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动网络"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
            else -> "未知"
        }

        val ipv4 = getLocalIpv4()
        val ipv6 = getLocalIpv6()

        val wifiInfo = if (networkType == "WiFi") {
            try { @Suppress("DEPRECATION") wm.connectionInfo } catch (e: Exception) {
                Log.w("NetworkInfoService", "Failed to get WiFi info", e)
                null
            }
        } else null

        NetworkInfo(
            networkType = networkType,
            ipv4 = ipv4,
            ipv6 = ipv6,
            gateway = getGateway(wm),
            dns = getDns(),
            ssid = wifiInfo?.ssid?.removeSurrounding("\""),
            signalStrength = wifiInfo?.rssi,
            linkSpeed = wifiInfo?.linkSpeed,
            macAddress = getMacAddress(),
        )
    }

    private fun getLocalIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        } catch (e: Exception) {
            Log.w("NetworkInfoService", "Failed to get IPv4 address", e)
            null
        }
    }

    private fun getLocalIpv6(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet6Address }
                ?.hostAddress
        } catch (e: Exception) {
            Log.w("NetworkInfoService", "Failed to get IPv6 address", e)
            null
        }
    }

    private fun getGateway(wm: WifiManager): String? {
        return try {
            val dhcp = wm.dhcpInfo
            if (dhcp != null && dhcp.gateway != 0) {
                formatIp(dhcp.gateway)
            } else null
        } catch (e: Exception) {
            Log.w("NetworkInfoService", "Failed to get Gateway", e)
            null
        }
    }

    private fun getDns(): String? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val linkProps = cm.getLinkProperties(cm.activeNetwork)
            linkProps?.dnsServers?.firstOrNull()?.hostAddress
        } catch (e: Exception) {
            Log.w("NetworkInfoService", "Failed to get DNS servers", e)
            null
        }
    }

    private fun getMacAddress(): String {
        return "随机 MAC / 系统提供值"
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
