package com.forgetmenot.app.assist

import android.app.assist.AssistStructure

/** A piece of text on screen and where it sits, in screen pixels. */
data class ScreenItem(val text: String, val left: Int, val top: Int, val width: Int, val height: Int) {
    val right get() = left + width
    val bottom get() = top + height
}

/** Pulls the visible text out of the app that was on screen, top to bottom, with positions. */
object ScreenText {
    private const val MAX_ITEMS = 400

    fun extract(structure: AssistStructure): List<ScreenItem> {
        val items = mutableListOf<ScreenItem>()
        for (i in 0 until structure.windowNodeCount) {
            val window = structure.getWindowNodeAt(i)
            walk(window.rootViewNode, window.left, window.top, items)
        }
        // Apps like Teams build chat lists bottom-up, so sort by where text actually sits on screen.
        return items.sortedWith(compareBy({ it.top }, { it.left })).take(MAX_ITEMS)
    }

    /** [left]/[top] are this node's position on screen. */
    private fun walk(node: AssistStructure.ViewNode, left: Int, top: Int, out: MutableList<ScreenItem>) {
        if (node.visibility != android.view.View.VISIBLE) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) out += ScreenItem(text, left, top, node.width, node.height)
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            walk(child, left + child.left - node.scrollX, top + child.top - node.scrollY, out)
        }
    }
}
