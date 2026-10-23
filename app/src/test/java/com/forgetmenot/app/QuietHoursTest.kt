package com.forgetmenot.app

import com.forgetmenot.app.reminders.QuietHours
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDateTime
import java.time.ZoneId

class QuietHoursTest {
    private val zone = ZoneId.of("America/Chicago")
    private val weekdays = DayOfWeek.entries.toSet() - SATURDAY - SUNDAY
    private val start = 19 * 60 + 30 // 7:30 PM
    private val end = 7 * 60 + 45 // 7:45 AM

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private fun adjust(text: String, days: Set<DayOfWeek> = weekdays, enabled: Boolean = true) =
        QuietHours.adjust(at(text), enabled, start, end, days, zone)

    @Test fun outsideQuietHoursIsUnchanged() =
        assertEquals(at("2026-10-21T14:00"), adjust("2026-10-21T14:00"))

    @Test fun eveningMovesToNextMorningWithMinutes() =
        assertEquals(at("2026-10-22T07:45"), adjust("2026-10-21T20:10"))

    @Test fun earlyMorningMovesToSameMorning() =
        assertEquals(at("2026-10-22T07:45"), adjust("2026-10-22T03:00"))

    @Test fun startMinuteIsQuiet() =
        assertEquals(at("2026-10-22T07:45"), adjust("2026-10-21T19:30"))

    @Test fun minuteBeforeStartIsAllowed() =
        assertEquals(at("2026-10-21T19:29"), adjust("2026-10-21T19:29"))

    @Test fun fridayEveningSkipsWeekendToMonday() {
        assertEquals(FRIDAY, LocalDateTime.parse("2026-10-23T20:00").dayOfWeek)
        assertEquals(at("2026-10-26T07:45"), adjust("2026-10-23T20:00"))
    }

    @Test fun saturdayAfternoonWaitsForMonday() =
        assertEquals(at("2026-10-26T07:45"), adjust("2026-10-24T13:00"))

    @Test fun daysOffApplyEvenWithQuietHoursOff() =
        assertEquals(at("2026-10-26T00:00"), adjust("2026-10-24T13:00", enabled = false))

    @Test fun sameDayWindow() {
        val lunch = QuietHours.adjust(at("2026-10-21T12:15"), true, 12 * 60, 13 * 60, setOf(MONDAY, DayOfWeek.WEDNESDAY), zone)
        assertEquals(at("2026-10-21T13:00"), lunch)
    }
}
