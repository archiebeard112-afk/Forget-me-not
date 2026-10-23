package com.forgetmenot.app.assist

import android.content.ComponentName
import android.content.Context
import android.service.voice.VoiceInteractionService

/**
 * Lets Forget-me-not be picked as the phone's digital assistant, so holding the
 * power button opens quick add with whatever is on screen.
 */
class AssistService : VoiceInteractionService() {
    companion object {
        fun isActive(context: Context): Boolean =
            isActiveService(context, ComponentName(context, AssistService::class.java))
    }
}
