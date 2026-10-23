package com.forgetmenot.app.assist

import android.app.assist.AssistStructure

/** Pulls the visible text out of the app that was on screen, top to bottom. */
object ScreenText {
    private const val MAX_CHARS = 20_000

    private class Item(val top: Int, val left: Int, val text: String)

    fun extract(structure: AssistStructure): String? {
        val items = mutableListOf<Item>()
        for (i in 0 until structure.windowNodeCount) {
            val window = structure.getWindowNodeAt(i)
            walk(window.rootViewNode, window.left, window.top, items)
        }
        // Apps like Teams build chat lists bottom-up, so sort by where text actually sits on screen.
        val lines = items.sortedWith(compareBy({ it.top }, { it.left })).map { it.text }
        return lines.joinToString("\n").take(MAX_CHARS).ifBlank { null }
    }

    /** [left]/[top] are this node's position on screen. */
    private fun walk(node: AssistStructure.ViewNode, left: Int, top: Int, out: MutableList<Item>) {
        if (node.visibility != android.view.View.VISIBLE) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) out += Item(top, left, text)
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            walk(child, left + child.left - node.scrollX, top + child.top - node.scrollY, out)
        }
    }
}
