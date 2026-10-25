package com.forgetmenot.app.assist

import android.service.voice.VoiceInteractionSession
import java.lang.ref.WeakReference

/** What the assist session captured, handed to the pop-up. */
object AssistInbox {
    data class Capture(val screenItems: List<ScreenItem>, val appPackage: String?)

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
