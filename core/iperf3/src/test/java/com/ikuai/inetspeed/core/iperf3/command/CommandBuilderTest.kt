package com.ikuai.inetspeed.core.iperf3.command

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandBuilderTest {

    @Test
    fun parseCliInputRemovesIperfBinaryAndKeepsArguments() {
        assertEquals(
            listOf("-c", "speed.example.com", "-p", "5201", "-u", "-b", "100M"),
            CommandBuilder.parseCliInput("iperf3 -c speed.example.com -p 5201 -u -b 100M"),
        )
    }

    @Test
    fun parseCliInputKeepsQuotedValuesTogether() {
        assertEquals(
            listOf("-c", "lab server.local", "-p", "5201"),
            CommandBuilder.parseCliInput("iperf3 -c \"lab server.local\" -p 5201"),
        )
    }
}
