package com.forgetmenot.app.reminders

import com.forgetmenot.app.data.Prefs
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object QuietHours {
    /** Moves [at] forward to the first moment nags are allowed: on a nag day and outside quiet hours. */
    fun adjust(at: Long, prefs: Prefs, zone: ZoneId = ZoneId.systemDefault()): Long =
        adjust(at, prefs.quietHoursEnabled, prefs.quietStartMinute, prefs.quietEndMinute, prefs.nagDays, zone)

    fun adjust(
        at: Long,
        quietEnabled: Boolean,
        start: Int,
        end: Int,
        nagDays: Set<DayOfWeek>,
        zone: ZoneId,
    ): Long {
        val days = nagDays.ifEmpty { DayOfWeek.entries.toSet() }
        val quiet = quietEnabled && start != end
        var t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), zone)
        repeat(16) {
            if (t.dayOfWeek !in days) {
                t = t.toLocalDate().plusDays(1).atStartOfDay(zone)
                return@repeat
            }
            val minute = t.hour * 60 + t.minute
            val inQuiet = quiet && if (start < end) minute in start until end else minute >= start || minute < end
            if (!inQuiet) return t.toInstant().toEpochMilli()
            var resume = t.toLocalDate().atStartOfDay(zone).plusMinutes(end.toLong())
            if (!resume.isAfter(t)) resume = resume.plusDays(1)
            t = resume
        }
        return t.toInstant().toEpochMilli()
    }
}
