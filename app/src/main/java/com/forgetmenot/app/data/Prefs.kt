package com.forgetmenot.app.data

import android.content.Context
import java.time.DayOfWeek

/** Small settings store. The API key stays on the phone and is only sent to Anthropic. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) = sp.edit().putString("api_key", v.trim()).apply()

    var model: String
        get() = sp.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(v) = sp.edit().putString("model", v).apply()

    var defaultNagMinutes: Int
        get() = sp.getInt("nag_minutes", 60)
        set(v) = sp.edit().putInt("nag_minutes", v).apply()

    var quietHoursEnabled: Boolean
        get() = sp.getBoolean("quiet_enabled", true)
        set(v) = sp.edit().putBoolean("quiet_enabled", v).apply()

    /** Minute of the day (0-1439) nags stop. */
    var quietStartMinute: Int
        get() = sp.getInt("quiet_start_min", sp.getInt("quiet_start", 19) * 60)
        set(v) = sp.edit().putInt("quiet_start_min", v).apply()

    /** Minute of the day (0-1439) nags resume. */
    var quietEndMinute: Int
        get() = sp.getInt("quiet_end_min", sp.getInt("quiet_end", 8) * 60)
        set(v) = sp.edit().putInt("quiet_end_min", v).apply()

    /** Days nags are allowed; other days are quiet all day. */
    var nagDays: Set<DayOfWeek>
        get() = sp.getStringSet("nag_days", null)?.map { DayOfWeek.valueOf(it) }?.toSet() ?: DayOfWeek.entries.toSet()
        set(v) = sp.edit().putStringSet("nag_days", v.map { it.name }.toSet()).apply()

    var pinnedQuickAdd: Boolean
        get() = sp.getBoolean("pinned_quick_add", false)
        set(v) = sp.edit().putBoolean("pinned_quick_add", v).apply()

    /** The "+ Task" tile and pinned notification add tasks over the lock screen (Save needs biometrics). */
    var lockScreenAdd: Boolean
        get() = sp.getBoolean("lock_screen_add", true)
        set(v) = sp.edit().putBoolean("lock_screen_add", v).apply()

    /** Shared or selected text is saved straight away instead of opening quick add. */
    var instantSave: Boolean
        get() = sp.getBoolean("instant_save", true)
        set(v) = sp.edit().putBoolean("instant_save", v).apply()

    /** When an instantly saved task is due; one of the DUE_* constants. */
    var instantDue: String
        get() = sp.getString("instant_due", DUE_IN_1H) ?: DUE_IN_1H
        set(v) = sp.edit().putString("instant_due", v).apply()

    /** What holding the power button does; one of the POWER_* constants. */
    var powerButtonMode: String
        get() = sp.getString("power_mode", POWER_HALO) ?: POWER_HALO
        set(v) = sp.edit().putString("power_mode", v).apply()

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"
        val MODELS = listOf(
            "claude-opus-5-5" to "Claude Opus 5.5 (most accurate)",
            "claude-haiku-5-5" to "Claude Haiku 5.5 (cheapest, fastest)",
        )

        const val DUE_IN_1H = "in_1h"
        const val DUE_5PM = "5pm"
        const val DUE_TOMORROW = "tomorrow"
        const val DUE_NONE = "none"
        val INSTANT_DUE_OPTIONS = listOf(
            DUE_IN_1H to "In 1 hour",
            DUE_5PM to "5pm today",
            DUE_TOMORROW to "Tomorrow 6am",
            DUE_NONE to "No due time",
        )

        const val POWER_HALO = "halo"
        const val POWER_PICK = "pick"
        const val POWER_CLAUDE = "claude"
    }
}
