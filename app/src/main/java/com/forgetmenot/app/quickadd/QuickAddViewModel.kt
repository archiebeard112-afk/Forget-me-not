package com.forgetmenot.app.quickadd

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forgetmenot.app.ForgetMeNotApp
import com.forgetmenot.app.assist.ScreenItem
import com.forgetmenot.app.assist.ScreenLines
import com.forgetmenot.app.data.DueDefaults
import com.forgetmenot.app.data.Prefs
import com.forgetmenot.app.data.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Phase {
    /** Type a task. */
    data object Input : Phase
    /** Tap the lines from the screen to save, as a list (power button). */
    data object Pick : Phase
    /** Tap the message right on the screen, inside a glowing halo (power button). */
    data object Halo : Phase
    /** A message picked from the screen, not saved yet: adjust, then Save reminder saves it. */
    data object Confirm : Phase
    /** Already saved (one-tap share); adjust or undo. */
    data object Saved : Phase
}

class QuickAddViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ForgetMeNotApp

    // Quick add form
    var input by mutableStateOf("")
    var dueAt by mutableStateOf(DueDefaults.instant(app.prefs))
    var nagMinutes by mutableStateOf(app.prefs.defaultNagMinutes)
    var highPriority by mutableStateOf(false)
    var clipboardSuggestion by mutableStateOf<String?>(null)
    var source = "Quick add"

    /** What was on screen, with positions; [screenLines] holds the same items' text, same order. */
    val screenItems = mutableStateListOf<ScreenItem>()
    val screenLines = mutableStateListOf<String>()
    val selectedLines = mutableStateListOf<Int>()
    private var screenApp: String? = null

    var phase by mutableStateOf<Phase>(Phase.Input)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var savedTask by mutableStateOf<Task?>(null)
        private set
    /** The task being confirmed in [Phase.Confirm]; not in the database yet. */
    var pendingTask by mutableStateOf<Task?>(null)
        private set
    private var pickedFrom: Phase = Phase.Pick

    fun useClipboard() {
        clipboardSuggestion?.let { input = it; source = "Copied message" }
        clipboardSuggestion = null
    }

    /** Called by the power-button pop-up with what was on screen. */
    fun loadFromScreen(items: List<ScreenItem>, appPackage: String?) {
        val cleaned = ScreenLines.clean(items)
        screenApp = appName(appPackage)
        source = screenApp?.let { "Screen ($it)" } ?: "Screen"
        screenItems.clear()
        screenItems += cleaned
        screenLines.clear()
        screenLines += cleaned.map { it.text }
        phase = when {
            screenLines.isEmpty() -> {
                error = "Couldn't read any text on that screen. Type the task instead."
                Phase.Input
            }
            app.prefs.powerButtonMode == Prefs.POWER_HALO -> Phase.Halo
            else -> Phase.Pick
        }
    }

    /** Switch from the halo to the list of the screen's text. */
    fun showList() {
        phase = Phase.Pick
    }

    /** Tapping a message: outline it for a moment, then open the pop-up to confirm it. */
    fun pick(index: Int) {
        if (selectedLines.isNotEmpty()) return // already opening
        selectedLines.add(index)
        pickedFrom = phase
        viewModelScope.launch {
            delay(250)
            pendingTask = draftTask(screenLines[index], ScreenLines.guessSender(screenLines, index))
            phase = Phase.Confirm
        }
    }

    fun updatePending(task: Task) {
        pendingTask = task
    }

    /** Save reminder: save the confirmed task. */
    fun confirmPending(onDone: () -> Unit) {
        val task = pendingTask ?: return
        if (task.title.isBlank()) return
        viewModelScope.launch {
            app.repository.save(task.copy(title = task.title.trim()))
            onDone()
        }
    }

    /** Back to the halo or list to pick a different message. */
    fun backToScreen() {
        pendingTask = null
        selectedLines.clear()
        phase = pickedFrom
    }

    /** One-tap save for shared or selected text. */
    fun saveShared(text: String) {
        input = text
        val task = draftTask(text, null)
        viewModelScope.launch {
            val id = app.repository.save(task)
            savedTask = task.copy(id = id)
            phase = Phase.Saved
        }
    }

    /** Change the just-saved task (due time, priority). */
    fun updateSaved(task: Task) {
        savedTask = task
        viewModelScope.launch { app.repository.save(task) }
    }

    fun undoSaved(onDone: () -> Unit) {
        val task = savedTask ?: return onDone()
        viewModelScope.launch {
            app.repository.delete(task.id)
            onDone()
        }
    }

    /** Save what's typed in quick add. */
    fun saveTyped(onDone: () -> Unit) {
        val text = input.trim()
        if (text.isEmpty()) return
        val firstLine = text.lineSequence().first().take(120)
        val task = Task(
            title = firstLine,
            notes = text.takeIf { it != firstLine },
            dueAt = dueAt,
            highPriority = highPriority,
            nagMinutes = nagMinutes,
            source = source,
        )
        viewModelScope.launch {
            app.repository.save(task)
            onDone()
        }
    }

    private fun draftTask(text: String, requester: String?): Task {
        val trimmed = text.trim()
        val firstLine = trimmed.lineSequence().first().take(120)
        return Task(
            title = firstLine,
            notes = trimmed.takeIf { it != firstLine },
            requester = requester,
            dueAt = DueDefaults.instant(app.prefs),
            nagMinutes = app.prefs.defaultNagMinutes,
            source = source,
        )
    }

    private fun appName(pkg: String?): String? = when {
        pkg == null -> null
        pkg.startsWith("com.microsoft.teams") -> "Teams"
        pkg.startsWith("com.microsoft.office.outlook") -> "Outlook"
        pkg.startsWith("com.Slack") -> "Slack"
        else -> null
    }
}
