package com.forgetmenot.app.quickadd

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/** Quick Settings "+ Task" tile, reachable from the lock screen. */
class QuickAddTileService : TileService() {
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(this, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launch = {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                @Suppress("DEPRECATION") startActivityAndCollapse(intent)
            }
        }
        // Work tasks stay behind the lock: ask to unlock first, then open quick add.
        if (isLocked) unlockAndRun(launch) else launch()
    }
}
