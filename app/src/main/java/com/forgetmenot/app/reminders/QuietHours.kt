package com.forgetmenot.app.reminders

import com.forgetmenot.app.data.Prefs
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object QuietHours {
    /** Moves [at] to the end of quiet hours if it falls inside them. */
    fun adjust(at: Long, prefs: Prefs, zone: ZoneId = ZoneId.systemDefault()): Long {
        if (!prefs.quietHoursEnabled || prefs.quietStart == prefs.quietEnd) return at
        val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), zone)
        val start = prefs.quietStart
        val end = prefs.quietEnd
        val h = t.hour
        val quiet = if (start < end) h in start until end else h >= start || h < end
        if (!quiet) return at
        var resume = t.withHour(end).withMinute(0).withSecond(0).withNano(0)
        if (!resume.isAfter(t)) resume = resume.plusDays(1)
        return resume.toInstant().toEpochMilli()
    }
}
