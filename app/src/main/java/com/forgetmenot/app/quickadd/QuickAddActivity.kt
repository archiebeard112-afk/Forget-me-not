package com.forgetmenot.app.quickadd

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.forgetmenot.app.MainActivity
import com.forgetmenot.app.ui.ForgetMeNotTheme

/**
 * Full-screen quick add: opened from the "+ Task" tile (unlocked), the pinned notification and the
 * New reminder button. Shared and selected text go to [CapturePopupActivity] instead.
 */
class QuickAddActivity : ComponentActivity() {
    private val vm: QuickAddViewModel by viewModels()
    private var checkedClipboard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            ForgetMeNotTheme {
                QuickAddScreen(vm, onClose = { finish() }, onEdit = ::openTask)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Android only lets an app read the clipboard while it has focus.
        if (hasFocus && !checkedClipboard) {
            checkedClipboard = true
            if (vm.input.isBlank()) {
                val clip = getSystemService(ClipboardManager::class.java).primaryClip
                val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
                if (!text.isNullOrBlank()) vm.clipboardSuggestion = text.trim()
            }
        }
    }

    private fun openTask(id: Long) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TASK_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    private fun handleShare(intent: Intent?) {
        intent?.getStringExtra(Intent.EXTRA_TEXT)?.let {
            vm.input = it
            vm.source = "Shared message"
            checkedClipboard = true
        }
    }
}
