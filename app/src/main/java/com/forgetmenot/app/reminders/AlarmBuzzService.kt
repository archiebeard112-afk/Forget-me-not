package com.forgetmenot.app.reminders

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Shows a reminder and vibrates like an alarm: a strong repeating pattern on the alarm channel,
 * so it buzzes even when the phone is on mute. Stops after [BUZZ_MS] or as soon as the reminder
 * is handled (Done, snooze, opened or swiped away). The reminder notification stays afterwards.
 */
class AlarmBuzzService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ID, 0) ?: 0
        val notification = intent?.let {
            if (Build.VERSION.SDK_INT >= 33) it.getParcelableExtra(EXTRA_NOTIFICATION, Notification::class.java)
            else @Suppress("DEPRECATION") it.getParcelableExtra(EXTRA_NOTIFICATION)
        }
        if (notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this, id, notification,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0,
        )
        running = this
        buzz(this)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ finish() }, BUZZ_MS)
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) = finish()

    /** Stop buzzing but leave the reminder notification showing. */
    fun finish() {
        handler.removeCallbacksAndMessages(null)
        vibrator(this).cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
        if (running === this) running = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (running === this) running = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_NOTIFICATION = "notification"
        private const val BUZZ_MS = 30_000L
        // Buzz, short gap, buzz, longer gap, repeated.
        private val PATTERN = longArrayOf(0, 900, 250, 900, 1200)

        private var running: AlarmBuzzService? = null

        /** Shows [notification] and starts the alarm vibration. Returns false if Android refused. */
        fun start(context: Context, id: Int, notification: Notification): Boolean = try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmBuzzService::class.java)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_NOTIFICATION, notification),
            )
            true
        } catch (_: Exception) {
            false
        }

        /** Stop any alarm vibration now. */
        fun stop(context: Context) {
            running?.finish()
            vibrator(context).cancel()
        }

        /** Vibrate on the alarm channel without a service (used if the service can't start). */
        fun buzz(context: Context) {
            val effect = VibrationEffect.createWaveform(PATTERN, 1)
            val vibrator = vibrator(context)
            if (Build.VERSION.SDK_INT >= 33) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        }

        private fun vibrator(context: Context): Vibrator =
            if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
            else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }
}
