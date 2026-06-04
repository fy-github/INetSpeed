package com.ikuai.inetspeed.feature.speedtest.components

fun formatSpeed(mbps: Double): String {
    return when {
        mbps >= 1000 -> String.format("%.1f", mbps / 1000)
        mbps >= 100 -> String.format("%.0f", mbps)
        mbps >= 10 -> String.format("%.1f", mbps)
        else -> String.format("%.2f", mbps)
    }
}
