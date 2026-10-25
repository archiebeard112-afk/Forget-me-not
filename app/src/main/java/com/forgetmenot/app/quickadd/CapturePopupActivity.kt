package com.forgetmenot.app.quickadd

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.forgetmenot.app.MainActivity
import com.forgetmenot.app.app
import com.forgetmenot.app.assist.AssistInbox
import com.forgetmenot.app.ui.ForgetMeNotTheme

/**
 * Shown over the current app: after holding the power button (halo: tap the message on
 * screen, or a list of the screen's text), or when text is shared / selected into
 * ForgetMeNot (saved in one tap).
 */
class CapturePopupActivity : ComponentActivity() {
    private val vm: QuickAddViewModel by viewModels()
    private var fromAssist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw under see-through system bars and the camera cutout, so the halo reaches every
        // edge; PopupSheet pads for the bars itself.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
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
                AssistInbox.take()?.let { vm.loadFromScreen(it.screenItems, it.appPackage) }
            }
        }
        setContent {
            ForgetMeNotTheme {
                if (vm.phase == Phase.Halo) {
                    HaloScreen(vm, onClose = { finish() })
                } else {
                    PopupSheet(onDismiss = { finish() }) {
                        QuickAddScreen(vm, onClose = { finish() }, onEdit = ::openTask, inSheet = true)
                    }
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
