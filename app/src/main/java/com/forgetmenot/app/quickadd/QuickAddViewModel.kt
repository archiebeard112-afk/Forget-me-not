package com.forgetmenot.app.quickadd

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forgetmenot.app.ForgetMeNotApp
import com.forgetmenot.app.ai.ExtractionException
import com.forgetmenot.app.ai.TaskExtractor
import com.forgetmenot.app.assist.ScreenItem
import com.forgetmenot.app.assist.ScreenLines
import com.forgetmenot.app.data.DueDefaults
import com.forgetmenot.app.data.Prefs
import com.forgetmenot.app.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** One draft task the user can tweak before saving. */
data class EditableDraft(
    val title: String,
    val requester: String = "",
    val details: String = "",
    val dueAt: Long? = null,
    val highPriority: Boolean = false,
    val warnings: List<String> = emptyList(),
    val include: Boolean = true,
)

sealed interface Phase {
    /** Type a task, or pick a screenshot. */
    data object Input : Phase
    /** Waiting for Claude. */
    data object Reading : Phase
    /** Check Claude's drafts. */
    data object Review : Phase
    /** Tap the lines from the screen to save, as a list (power button, no AI). */
    data object Pick : Phase
    /** Tap the message right on the screen, inside a glowing halo (power button, no AI). */
    data object Halo : Phase
    /** Already saved; adjust or undo. */
    data object Saved : Phase
}

class QuickAddViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ForgetMeNotApp

    var input by mutableStateOf("")
    var dueAt by mutableStateOf<Long?>(null)
    var image by mutableStateOf<Bitmap?>(null)
        private set
    /** Text read off the screen when the power button was held. */
    var screenText by mutableStateOf<String?>(null)
        private set
    private var screenApp: String? = null
    /** What was on screen, with positions; [screenLines] holds the same items' text, same order. */
    val screenItems = mutableStateListOf<ScreenItem>()
    val screenLines = mutableStateListOf<String>()
    val selectedLines = mutableStateListOf<Int>()
    var clipboardSuggestion by mutableStateOf<String?>(null)
    var phase by mutableStateOf<Phase>(Phase.Input)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val drafts = mutableStateListOf<EditableDraft>()
    val skipped = mutableStateListOf<String>()
    var savedTask by mutableStateOf<Task?>(null)
        private set
    var source = "Quick add"

    val hasApiKey get() = app.prefs.apiKey.isNotBlank()

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            image = withContext(Dispatchers.IO) {
                app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let(::shrink)
            }
            source = "Screenshot"
            if (image == null) error = "Couldn't open that image."
        }
    }

    fun clearImage() {
        image = null
    }

    fun useClipboard() {
        clipboardSuggestion?.let { input = it; source = "Copied message" }
        clipboardSuggestion = null
    }

    /** Called by the power-button pop-up with what was on screen. */
    fun loadFromScreen(items: List<ScreenItem>, screenshot: Bitmap?, appPackage: String?) {
        val cleaned = ScreenLines.clean(items)
        val text = cleaned.joinToString("\n") { it.text }.ifBlank { null }
        screenText = text
        image = screenshot?.let(::shrink)
        screenApp = appName(appPackage)
        source = screenApp?.let { "Screen ($it)" } ?: "Screen"
        if (app.prefs.powerButtonMode == Prefs.POWER_CLAUDE && hasApiKey && (text != null || image != null)) {
            read()
            return
        }
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

    fun clearScreen() {
        screenText = null
        image = null
        screenItems.clear()
        screenLines.clear()
        selectedLines.clear()
    }

    fun toggleLine(index: Int) {
        if (index in selectedLines) selectedLines.remove(index) else selectedLines.add(index)
    }

    /** Save the lines picked from the screen. */
    fun savePicked() {
        if (selectedLines.isEmpty()) return
        val sorted = selectedLines.sorted()
        val text = sorted.joinToString("\n") { screenLines[it] }
        saveNow(text, ScreenLines.guessSender(screenLines, sorted.first()))
    }

    /** One-tap save for shared or selected text. */
    fun saveShared(text: String) {
        input = text
        saveNow(text, null)
    }

    private fun saveNow(text: String, requester: String?) {
        val trimmed = text.trim()
        val firstLine = trimmed.lineSequence().first().take(120)
        val task = Task(
            title = firstLine,
            notes = trimmed.takeIf { it != firstLine },
            requester = requester,
            dueAt = DueDefaults.instant(app.prefs),
            nagMinutes = app.prefs.defaultNagMinutes,
            source = source,
        )
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

    /** Ask Claude to pull tasks out of the text, screen and/or screenshot. */
    fun read() {
        val text = input.takeIf { it.isNotBlank() }
        val img = image
        val screen = screenText
        if (text == null && img == null && screen == null) return
        error = null
        phase = Phase.Reading
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val b64 = img?.let {
                        val out = ByteArrayOutputStream()
                        it.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    }
                    TaskExtractor(app.prefs.apiKey, app.prefs.model)
                        .extract(text, screen, screenApp, b64, "image/jpeg")
                }
                drafts.clear()
                skipped.clear()
                result.tasks.forEach {
                    drafts += EditableDraft(
                        title = it.title,
                        requester = it.requester ?: "",
                        details = it.details ?: "",
                        dueAt = it.dueAt,
                        highPriority = it.highPriority,
                        warnings = it.warnings,
                    )
                }
                skipped += result.skipped
                if (drafts.isEmpty()) {
                    error = "Claude didn't find a request in this. You can type the task instead."
                    phase = inputPhase()
                } else {
                    phase = Phase.Review
                }
            } catch (e: ExtractionException) {
                error = e.message
                phase = inputPhase()
            } catch (e: Exception) {
                error = "Couldn't reach Claude: ${e.message ?: e.javaClass.simpleName}"
                phase = inputPhase()
            }
        }
    }

    fun backToInput() {
        phase = inputPhase()
    }

    private fun inputPhase(): Phase = if (screenLines.isNotEmpty()) Phase.Pick else Phase.Input

    /** Save what's typed without asking Claude. */
    fun saveTyped(onDone: () -> Unit) {
        val text = input.trim()
        if (text.isEmpty()) return
        val firstLine = text.lineSequence().first().take(120)
        val task = Task(
            title = firstLine,
            notes = text.takeIf { it != firstLine },
            dueAt = dueAt,
            nagMinutes = app.prefs.defaultNagMinutes,
            source = source,
        )
        viewModelScope.launch {
            app.repository.save(task)
            onDone()
        }
    }

    fun saveDrafts(onDone: () -> Unit) {
        val notes = input.takeIf { it.isNotBlank() }
        val toSave = drafts.filter { it.include && it.title.isNotBlank() }.map {
            Task(
                title = it.title.trim(),
                requester = it.requester.trim().ifEmpty { null },
                details = it.details.trim().ifEmpty { null },
                notes = notes,
                dueAt = it.dueAt,
                highPriority = it.highPriority,
                nagMinutes = if (it.highPriority) minOf(30, app.prefs.defaultNagMinutes) else app.prefs.defaultNagMinutes,
                source = source,
            )
        }
        viewModelScope.launch {
            toSave.forEach { app.repository.save(it) }
            onDone()
        }
    }

    private fun shrink(original: Bitmap): Bitmap {
        // Screenshots from the system can be hardware bitmaps, which can't be scaled directly.
        val src = if (original.config == Bitmap.Config.HARDWARE) original.copy(Bitmap.Config.ARGB_8888, false) else original
        val longEdge = maxOf(src.width, src.height)
        if (longEdge <= MAX_EDGE) return src
        val scale = MAX_EDGE.toFloat() / longEdge
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }

    private fun appName(pkg: String?): String? = when {
        pkg == null -> null
        pkg.startsWith("com.microsoft.teams") -> "Teams"
        pkg.startsWith("com.microsoft.office.outlook") -> "Outlook"
        pkg.startsWith("com.Slack") -> "Slack"
        else -> null
    }

    private companion object {
        const val MAX_EDGE = 2000
    }
}
