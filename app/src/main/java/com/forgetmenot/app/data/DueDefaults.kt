package com.forgetmenot.app.data

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object DueDefaults {
    /** "Tomorrow" everywhere in the app means tomorrow at this time. */
    val TOMORROW_TIME: LocalTime = LocalTime.of(6, 0)

    fun tomorrowMorning(zone: ZoneId = ZoneId.systemDefault()): Long =
        tomorrowMorning(ZonedDateTime.now(zone)).toInstant().toEpochMilli()

    private fun tomorrowMorning(now: ZonedDateTime): ZonedDateTime =
        now.toLocalDate().plusDays(1).atTime(TOMORROW_TIME).atZone(now.zone)

    /** Due time for a task saved in one tap, per the user's setting. */
    fun instant(prefs: Prefs, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val due = when (prefs.instantDue) {
            Prefs.DUE_IN_1H -> now.plusHours(1)
            Prefs.DUE_5PM -> if (now.toLocalTime() < LocalTime.of(17, 0)) now.with(LocalTime.of(17, 0)) else tomorrowMorning(now)
            Prefs.DUE_TOMORROW -> tomorrowMorning(now)
            else -> null
        }
        return due?.toInstant()?.toEpochMilli()
    }
}
