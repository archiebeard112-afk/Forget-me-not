package com.forgetmenot.app.assist

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import androidx.annotation.RequiresApi
import com.forgetmenot.app.quickadd.CapturePopupActivity

/**
 * Runs when the power button is held. Collects the screen's text and where it sits (and a
 * screenshot when the user allows it), then opens the pop-up over the current app.
 */
class AssistSession(context: Context) : VoiceInteractionSession(context) {
    private val handler = Handler(Looper.getMainLooper())
    private var wantScreenshot = false
    private var screenItems: List<ScreenItem> = emptyList()
    private var appPackage: String? = null
    private var launched = false

    override fun onPrepareShow(args: Bundle?, showFlags: Int) {
        super.onPrepareShow(args, showFlags)
        // The pop-up is an activity; no session window needed.
        setUiEnabled(false)
        launched = false
        screenItems = emptyList()
        appPackage = null
        wantScreenshot = showFlags and SHOW_WITH_SCREENSHOT != 0
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (showFlags and (SHOW_WITH_ASSIST or SHOW_WITH_SCREENSHOT) == 0) {
            launch(null)
        } else {
            // Don't leave the user waiting if the system never delivers the screen.
            handler.postDelayed({ launch(null) }, 2_500)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onHandleAssist(state: AssistState) {
        if (state.index == 0) handleStructure(state.assistStructure)
    }

    @Deprecated("Called on Android 9 and below")
    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        handleStructure(structure)
    }

    private fun handleStructure(structure: AssistStructure?) {
        if (structure != null) {
            screenItems = ScreenText.extract(structure)
            appPackage = structure.activityComponent?.packageName
        }
        if (!wantScreenshot) launch(null)
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        launch(screenshot)
    }

    private fun launch(screenshot: Bitmap?) {
        if (launched) return
        launched = true
        handler.removeCallbacksAndMessages(null)
        AssistInbox.put(AssistInbox.Capture(screenItems, screenshot, appPackage), this)
        val intent = Intent(context, CapturePopupActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            startAssistantActivity(intent)
        } else {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onHide() {
        super.onHide()
        handler.removeCallbacksAndMessages(null)
    }
}
