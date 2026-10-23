package com.forgetmenot.app

import com.forgetmenot.app.assist.ScreenLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenLinesTest {
    private val screen = """
        Extract Orders
        8 participants
        Autumn Larson
        thank you!
        Monday 10:34 AM
        Autumn Larson
        extract order opened for zents, please. 5199g of EV25.OBXD.325 in a 2-gallon bucket.
        extract order opened for zents, please. 5199g of EV25.OBXD.325 in a 2-gallon bucket.
        Type a message
    """.trimIndent()

    @Test fun splitsTrimsAndDropsRepeats() {
        val lines = ScreenLines.split(screen)
        assertEquals(8, lines.size)
        assertEquals("Type a message", lines.last())
    }

    @Test fun findsSenderAboveMessage() {
        val lines = ScreenLines.split(screen)
        val message = lines.indexOfFirst { it.startsWith("extract order") }
        assertEquals("Autumn Larson", ScreenLines.guessSender(lines, message))
    }

    @Test fun ignoresLinesThatAreNotNames() {
        val lines = listOf("Monday 10:34 AM", "please send the numbers")
        assertNull(ScreenLines.guessSender(lines, 1))
    }
}
