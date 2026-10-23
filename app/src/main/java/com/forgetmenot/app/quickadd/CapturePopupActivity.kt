package com.forgetmenot.app.quickadd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.forgetmenot.app.MainActivity
import com.forgetmenot.app.app
import com.forgetmenot.app.assist.AssistInbox
import com.forgetmenot.app.ui.ForgetMeNotTheme

/**
 * The pop-up shown over the current app: after holding the power button (pick lines from
 * the screen), or when text is shared / selected into Forget-me-not (saved in one tap).
 */
class CapturePopupActivity : ComponentActivity() {
    private val vm: QuickAddViewModel by viewModels()
    private var fromAssist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Same layout on every Android version; PopupSheet pads for the system bars itself.
        enableEdgeToEdge()
        val sharedText = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        fromAssist = intent.action == null
        if (savedInstanceState == null) {
            if (sharedText != null) {
                vm.source = if (intent.action == Intent.ACTION_PROCESS_TEXT) "Selected text" else "Shared message"
                if (app.prefs.instantSave && sharedText.isNotBlank()) vm.saveShared(sharedText) else vm.input = sharedText
            } else {
                AssistInbox.take()?.let { vm.loadFromScreen(it.screenText, it.screenshot, it.appPackage) }
            }
        }
        setContent {
            ForgetMeNotTheme {
                PopupSheet(onDismiss = { finish() }) {
                    QuickAddScreen(vm, onClose = { finish() }, onEdit = ::openTask, inSheet = true)
                }
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

    override fun onDestroy() {
        super.onDestroy()
        if (fromAssist && isFinishing) AssistInbox.finishSession()
    }
}
