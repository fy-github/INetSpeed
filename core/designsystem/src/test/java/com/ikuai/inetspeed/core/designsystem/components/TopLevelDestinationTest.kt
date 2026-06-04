package com.ikuai.inetspeed.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun topLevelDestinationsUseDocumentedIconSemantics() {
        val semantics = TopLevelDestination.entries.associate { it.label to it.iconSemantic }

        assertEquals("waveform", semantics.getValue("测速"))
        assertEquals("monitor", semantics.getValue("工具"))
        assertEquals("bars", semantics.getValue("历史"))
        assertEquals("document", semantics.getValue("报告"))
        assertEquals("gear", semantics.getValue("设置"))
    }
}
