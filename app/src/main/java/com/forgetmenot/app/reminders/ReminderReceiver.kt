package com.forgetmenot.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forgetmenot.app.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
                when (intent.action) {
                    ACTION_FIRE -> {
                        val task = repo.get(taskId) ?: return@launch
                        if (task.done) return@launch
                        Notifications.showReminder(context, task)
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
                        val zone = ZoneId.systemDefault()
                        val at = LocalDate.now(zone).plusDays(1).atTime(LocalTime.of(9, 0))
                            .atZone(zone).toInstant().toEpochMilli()
                        repo.snooze(taskId, at)
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
    }
}
