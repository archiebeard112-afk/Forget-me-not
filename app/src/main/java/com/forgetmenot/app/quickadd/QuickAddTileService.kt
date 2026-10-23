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
        // Locked: the lock-screen pop-up (it asks to unlock instead if that's turned off in Settings).
        val target = if (isLocked) LockScreenAddActivity::class.java else QuickAddActivity::class.java
        val intent = Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launch = {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                @Suppress("DEPRECATION") startActivityAndCollapse(intent)
            }
        }
        launch()
    }
}
