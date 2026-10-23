package com.forgetmenot.app.assist

import android.app.assist.AssistStructure

/** Pulls the visible text out of the app that was on screen, in screen order. */
object ScreenText {
    private const val MAX_CHARS = 20_000

    fun extract(structure: AssistStructure): String? {
        val lines = mutableListOf<String>()
        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode, lines)
        }
        return lines.joinToString("\n").take(MAX_CHARS).ifBlank { null }
    }

    private fun walk(node: AssistStructure.ViewNode, out: MutableList<String>) {
        if (node.visibility != android.view.View.VISIBLE) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && out.lastOrNull() != text) out += text
        for (i in 0 until node.childCount) walk(node.getChildAt(i), out)
    }
}
