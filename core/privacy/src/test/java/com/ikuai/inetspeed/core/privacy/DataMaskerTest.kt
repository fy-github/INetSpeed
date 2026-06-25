package com.ikuai.inetspeed.core.privacy

import org.junit.Assert.assertEquals
import org.junit.Test

class DataMaskerTest {

    private val masker = DataMasker()

    @Test
    fun maskIpv4_masksLastTwoOctets() {
        assertEquals("192.168.*.**", masker.maskIpv4("192.168.1.100"))
    }

    @Test
    fun maskIpv4_returnsOriginalIfInvalid() {
        assertEquals("invalid", masker.maskIpv4("invalid"))
    }

    @Test
    fun maskIpv6_masksLastSegments() {
        assertEquals("2001:db8::****", masker.maskIpv6("2001:db8::1"))
    }

    @Test
    fun maskDomain_masksMiddlePart() {
        assertEquals("hk-iperf.***.com", masker.maskDomain("hk-iperf.ikuai.com"))
    }

    @Test
    fun maskDomain_returnsOriginalIfSinglePart() {
        assertEquals("localhost", masker.maskDomain("localhost"))
    }

    @Test
    fun maskRouteHops_masksIpAddresses() {
        val input = "1  192.168.1.1  10ms\n2  10.0.0.1  20ms"
        val expected = "1  192.168.*.**  10ms\n2  10.0.*.**  20ms"
        assertEquals(expected, masker.maskRouteHops(input))
    }

    @Test
    fun applyPrivacySettings_masksIpWhenDisabled() {
        val text = "Server 192.168.1.100 connected"
        val result = masker.applyPrivacySettings(text, includeIp = false, includeDomain = true, includeRoute = true)
        assertEquals("Server 192.168.*.** connected", result)
    }

    @Test
    fun applyPrivacySettings_masksDomainWhenDisabled() {
        val text = "Connected to hk-iperf.ikuai.com"
        val result = masker.applyPrivacySettings(text, includeIp = true, includeDomain = false, includeRoute = true)
        assertEquals("Connected to hk-iperf.***.com", result)
    }

    @Test
    fun applyPrivacySettings_preservesWhenEnabled() {
        val text = "Server 192.168.1.100 at hk-iperf.ikuai.com"
        val result = masker.applyPrivacySettings(text, includeIp = true, includeDomain = true, includeRoute = true)
        assertEquals(text, result)
    }
}
