package com.forgetmenot.app.data

import java.time.LocalTime
import java.time.ZonedDateTime

object DueDefaults {
    /** Due time for a task saved in one tap, per the user's setting. */
    fun instant(prefs: Prefs, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val tomorrow9 = now.toLocalDate().plusDays(1).atTime(9, 0).atZone(now.zone)
        val due = when (prefs.instantDue) {
            Prefs.DUE_IN_1H -> now.plusHours(1)
            Prefs.DUE_5PM -> if (now.toLocalTime() < LocalTime.of(17, 0)) now.with(LocalTime.of(17, 0)) else tomorrow9
            Prefs.DUE_TOMORROW -> tomorrow9
            else -> null
        }
        return due?.toInstant()?.toEpochMilli()
    }
}
