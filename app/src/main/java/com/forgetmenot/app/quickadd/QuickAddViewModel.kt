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
    data object Input : Phase
    data object Reading : Phase
    data object Review : Phase
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
    var clipboardSuggestion by mutableStateOf<String?>(null)
    var phase by mutableStateOf<Phase>(Phase.Input)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val drafts = mutableStateListOf<EditableDraft>()
    val skipped = mutableStateListOf<String>()
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

    /** Called by the power-button pop-up with what was on screen; starts reading right away. */
    fun loadFromScreen(text: String?, screenshot: Bitmap?, appPackage: String?) {
        screenText = text
        image = screenshot?.let(::shrink)
        screenApp = appName(appPackage)
        source = screenApp?.let { "Screen ($it)" } ?: "Screen"
        if (hasApiKey && (text != null || image != null)) read()
    }

    fun clearScreen() {
        screenText = null
        image = null
    }

    fun useClipboard() {
        clipboardSuggestion?.let { input = it; source = "Copied message" }
        clipboardSuggestion = null
    }

    /** Ask Claude to pull tasks out of the text and/or screenshot. */
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
                    phase = Phase.Input
                } else {
                    phase = Phase.Review
                }
            } catch (e: ExtractionException) {
                error = e.message
                phase = Phase.Input
            } catch (e: Exception) {
                error = "Couldn't reach Claude: ${e.message ?: e.javaClass.simpleName}"
                phase = Phase.Input
            }
        }
    }

    fun backToInput() {
        phase = Phase.Input
    }

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
