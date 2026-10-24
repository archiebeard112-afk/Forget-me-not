package com.forgetmenot.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forgetmenot.app.app
import com.forgetmenot.app.data.DueDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires reminders and handles the Done / +1 hour / Tomorrow notification buttons. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId < 0) return
        val pending = goAsync()
        val app = context.app
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = app.repository
                // Any answer to a reminder stops the alarm vibration.
                if (intent.action != ACTION_FIRE) AlarmBuzzService.stop(context)
                when (intent.action) {
                    ACTION_FIRE -> {
                        val task = repo.get(taskId) ?: return@launch
                        if (task.done) return@launch
                        Notifications.showReminder(context, task, app.prefs.alarmVibration)
                        // Keep nagging until the task is done or snoozed.
                        val next = QuietHours.adjust(
                            System.currentTimeMillis() + task.nagMinutes * 60_000L, app.prefs,
                        )
                        repo.scheduleNextNag(task, next)
                    }
                    ACTION_DONE -> {
                        repo.setDone(taskId, true)
                        Notifications.cancel(context, taskId)
                    }
                    ACTION_SNOOZE_HOUR -> {
                        repo.snooze(taskId, System.currentTimeMillis() + 60 * 60_000L)
                        Notifications.cancel(context, taskId)
                    }
                    ACTION_TOMORROW -> {
                        repo.snooze(taskId, DueDefaults.tomorrowMorning())
                        Notifications.cancel(context, taskId)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_FIRE = "com.forgetmenot.app.FIRE"
        const val ACTION_DONE = "com.forgetmenot.app.DONE"
        const val ACTION_SNOOZE_HOUR = "com.forgetmenot.app.SNOOZE_HOUR"
        const val ACTION_TOMORROW = "com.forgetmenot.app.TOMORROW"
        const val ACTION_DISMISSED = "com.forgetmenot.app.DISMISSED"
    }
}
