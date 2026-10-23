package com.forgetmenot.app.assist

import android.graphics.Bitmap
import android.service.voice.VoiceInteractionSession
import java.lang.ref.WeakReference

/** What the assist session captured, handed to the pop-up (a screenshot is too big for an Intent). */
object AssistInbox {
    data class Capture(val screenText: String?, val screenshot: Bitmap?, val appPackage: String?)

    private var capture: Capture? = null
    private var session: WeakReference<VoiceInteractionSession>? = null

    fun put(capture: Capture, session: VoiceInteractionSession) {
        this.capture = capture
        this.session = WeakReference(session)
    }

    fun take(): Capture? = capture.also { capture = null }

    fun finishSession() {
        session?.get()?.finish()
        session = null
    }
}
