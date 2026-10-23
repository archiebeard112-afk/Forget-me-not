package com.forgetmenot.app.assist

/** Tidies captured screen text into items the user can tap. */
object ScreenLines {
    private val NAME = Regex("""^\p{Lu}[\p{L}'’.-]+( \p{Lu}[\p{L}'’.-]*){1,3}$""")

    /** Drops blank, one-character and invisible items, and repeats of the item just above. */
    fun clean(items: List<ScreenItem>): List<ScreenItem> {
        val out = mutableListOf<ScreenItem>()
        for (item in items) {
            val text = item.text.trim()
            if (text.length < 2 || item.width <= 0 || item.height <= 0) continue
            if (out.lastOrNull()?.text == text) continue
            out += item.copy(text = text)
        }
        return out
    }

    /**
     * The item under a tap at screen position [x], [y], allowing [slop] pixels of miss.
     * When items overlap (a quote inside a message), the smallest one wins.
     */
    fun itemAt(items: List<ScreenItem>, x: Float, y: Float, slop: Float): Int? =
        items.indices
            .filter {
                val item = items[it]
                x >= item.left - slop && x <= item.right + slop && y >= item.top - slop && y <= item.bottom + slop
            }
            .minByOrNull { items[it].width.toLong() * items[it].height }

    /** Chat apps show the sender's name just above a message; use it if it looks like a name. */
    fun guessSender(lines: List<String>, messageIndex: Int): String? =
        (messageIndex - 1 downTo maxOf(0, messageIndex - 3))
            .map { lines[it] }
            .firstOrNull { NAME.matches(it) && it.length <= 40 && it != "You" }
}
