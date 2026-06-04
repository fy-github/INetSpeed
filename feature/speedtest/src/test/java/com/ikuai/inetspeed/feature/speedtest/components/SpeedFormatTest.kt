package com.ikuai.inetspeed.feature.speedtest.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatTest {
    @Test
    fun formatsGigabitSpeedsAsGbps() {
        assertEquals("1.2", formatSpeed(1234.0))
    }

    @Test
    fun formatsSubTenSpeedsWithTwoDecimals() {
        assertEquals("2.35", formatSpeed(2.345))
    }
}
