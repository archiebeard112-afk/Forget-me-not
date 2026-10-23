package com.forgetmenot.app.quickadd

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.forgetmenot.app.ui.ForgetMeNotTheme

/**
 * Fast capture: opened from the "+ Task" tile, the pinned notification, the Add button,
 * the share sheet (text or screenshot), or the text-selection menu.
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
                QuickAddScreen(vm, onClose = { finish() })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Android only lets an app read the clipboard while it has focus.
        if (hasFocus && !checkedClipboard) {
            checkedClipboard = true
            if (vm.input.isBlank() && vm.image == null) {
                val clip = getSystemService(ClipboardManager::class.java).primaryClip
                val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
                if (!text.isNullOrBlank()) vm.clipboardSuggestion = text.trim()
            }
        }
    }

    private fun handleShare(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val stream = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (stream != null && intent.type?.startsWith("image/") == true) {
                    vm.loadImage(stream)
                } else {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { vm.input = it; vm.source = "Shared message" }
                }
                checkedClipboard = true
            }
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
                    vm.input = it.toString(); vm.source = "Selected text"
                }
                checkedClipboard = true
            }
        }
    }
}
