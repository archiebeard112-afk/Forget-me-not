package com.forgetmenot.app.assist

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/** Android requires an assistant to declare a speech recognizer. We don't do voice. */
class StubRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onCancel(listener: Callback?) {}

    override fun onStopListening(listener: Callback?) {}
}
