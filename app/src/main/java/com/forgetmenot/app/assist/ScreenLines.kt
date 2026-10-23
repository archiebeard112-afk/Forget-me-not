package com.forgetmenot.app.assist

/** Turns captured screen text into lines the user can tap. */
object ScreenLines {
    private val NAME = Regex("""^\p{Lu}[\p{L}'’.-]+( \p{Lu}[\p{L}'’.-]*){1,3}$""")

    fun split(text: String?): List<String> {
        if (text == null) return emptyList()
        val out = mutableListOf<String>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.length >= 2 && out.lastOrNull() != line) out += line
        }
        return out
    }

    /** Chat apps show the sender's name just above a message; use it if it looks like a name. */
    fun guessSender(lines: List<String>, messageIndex: Int): String? =
        (messageIndex - 1 downTo maxOf(0, messageIndex - 3))
            .map { lines[it] }
            .firstOrNull { NAME.matches(it) && it.length <= 40 && it != "You" }
}
