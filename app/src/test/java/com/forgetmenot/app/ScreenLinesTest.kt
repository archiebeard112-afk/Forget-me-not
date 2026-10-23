package com.forgetmenot.app

import com.forgetmenot.app.assist.ScreenItem
import com.forgetmenot.app.assist.ScreenLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenLinesTest {
    private val screen = listOf(
        "Extract Orders",
        "8 participants",
        "Autumn Larson",
        "thank you!",
        "Monday 10:34 AM",
        "Autumn Larson",
        "extract order opened for zents, please. 5199g of EV25.OBXD.325 in a 2-gallon bucket.",
        "extract order opened for zents, please. 5199g of EV25.OBXD.325 in a 2-gallon bucket.",
        " ",
        "Type a message",
    ).mapIndexed { i, text -> ScreenItem(text, 0, i * 100, 500, 80) }

    @Test fun dropsBlanksAndRepeats() {
        val items = ScreenLines.clean(screen)
        assertEquals(8, items.size)
        assertEquals("Type a message", items.last().text)
    }

    @Test fun dropsInvisibleItems() {
        val hidden = ScreenItem("hidden", 0, 0, 0, 0)
        assertEquals(emptyList<ScreenItem>(), ScreenLines.clean(listOf(hidden)))
    }

    @Test fun findsSenderAboveMessage() {
        val lines = ScreenLines.clean(screen).map { it.text }
        val message = lines.indexOfFirst { it.startsWith("extract order") }
        assertEquals("Autumn Larson", ScreenLines.guessSender(lines, message))
    }

    @Test fun ignoresLinesThatAreNotNames() {
        val lines = listOf("Monday 10:34 AM", "please send the numbers")
        assertNull(ScreenLines.guessSender(lines, 1))
    }
}

class HaloHitTest {
    private val bubble = ScreenItem("whole message with a quote", 100, 1000, 800, 400)
    private val quote = ScreenItem("quoted text", 140, 1040, 700, 120)
    private val name = ScreenItem("Autumn Larson", 100, 900, 300, 60)
    private val items = listOf(name, bubble, quote)

    @Test fun tapOnMessageSelectsIt() = assertEquals(1, ScreenLines.itemAt(items, 500f, 1300f, 20f))

    @Test fun overlappingPicksTheSmallest() = assertEquals(2, ScreenLines.itemAt(items, 500f, 1100f, 20f))

    @Test fun nearMissWithinSlopCounts() = assertEquals(0, ScreenLines.itemAt(items, 90f, 930f, 20f))

    @Test fun emptySpaceSelectsNothing() = assertNull(ScreenLines.itemAt(items, 950f, 500f, 20f))
}
