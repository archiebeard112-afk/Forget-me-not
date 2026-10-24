package com.forgetmenot.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.forgetmenot.app.MainActivity
import com.forgetmenot.app.R
import com.forgetmenot.app.data.Task
import com.forgetmenot.app.quickadd.LockScreenAddActivity
import com.forgetmenot.app.ui.formatDue

object Notifications {
    private const val CHANNEL_REMINDERS = "reminders"
    private const val CHANNEL_QUICK_ADD = "quick_add"
    private const val QUICK_ADD_ID = Int.MAX_VALUE

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nagging reminders for open tasks"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_QUICK_ADD, "Quick add", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Pinned \"+ Add task\" shortcut"
                setShowBadge(false)
            }
        )
    }

    /** Shows the reminder; with alarm vibration on, it also buzzes like an alarm. */
    fun showReminder(context: Context, task: Task, alarmVibration: Boolean = false) {
        val id = task.id.toInt()
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TASK_ID, task.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = listOfNotNull(
            task.details,
            task.requester?.let { "From $it" },
            task.dueAt?.let { formatDue(it) },
        ).joinToString(" · ")
        val title = (if (task.highPriority) "🔴 " else "") + task.title
        val n = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Done", action(context, task.id, ReminderReceiver.ACTION_DONE))
            .addAction(0, "+1 hour", action(context, task.id, ReminderReceiver.ACTION_SNOOZE_HOUR))
            .addAction(0, "Tomorrow", action(context, task.id, ReminderReceiver.ACTION_TOMORROW))
            // Swiping it away stops the alarm vibration.
            .setDeleteIntent(action(context, task.id, ReminderReceiver.ACTION_DISMISSED))
            .build()
        if (alarmVibration && AlarmBuzzService.start(context, id, n)) return
        if (alarmVibration) AlarmBuzzService.buzz(context)
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(id, n)
            } catch (_: SecurityException) {
            }
        }
    }

    fun cancel(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }

    fun showQuickAdd(context: Context) {
        val pi = PendingIntent.getActivity(
            context, QUICK_ADD_ID,
            // Opens over the lock screen when locked, or the normal quick add when unlocked.
            Intent(context, LockScreenAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_QUICK_ADD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("+ Add task")
            .setContentText("Tap to capture something before you forget it")
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(QUICK_ADD_ID, n)
        } catch (_: SecurityException) {
        }
    }

    fun hideQuickAdd(context: Context) {
        NotificationManagerCompat.from(context).cancel(QUICK_ADD_ID)
    }

    private fun action(context: Context, taskId: Long, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            taskId.toInt() * 10 + when (action) {
                ReminderReceiver.ACTION_DONE -> 1
                ReminderReceiver.ACTION_SNOOZE_HOUR -> 2
                ReminderReceiver.ACTION_TOMORROW -> 3
                else -> 4
            },
            Intent(context, ReminderReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
