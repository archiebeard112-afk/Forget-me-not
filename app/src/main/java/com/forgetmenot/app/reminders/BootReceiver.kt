package com.forgetmenot.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forgetmenot.app.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are wiped on reboot and app update, so book them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.app.repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
