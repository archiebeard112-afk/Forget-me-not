package com.forgetmenot.app.data

import android.content.Context

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

    /** Hour of day (0-23) nags stop. */
    var quietStart: Int
        get() = sp.getInt("quiet_start", 19)
        set(v) = sp.edit().putInt("quiet_start", v).apply()

    /** Hour of day (0-23) nags resume. */
    var quietEnd: Int
        get() = sp.getInt("quiet_end", 8)
        set(v) = sp.edit().putInt("quiet_end", v).apply()

    var pinnedQuickAdd: Boolean
        get() = sp.getBoolean("pinned_quick_add", false)
        set(v) = sp.edit().putBoolean("pinned_quick_add", v).apply()

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"
        val MODELS = listOf(
            "claude-opus-5-5" to "Claude Opus 5.5 (most accurate)",
            "claude-haiku-5-5" to "Claude Haiku 5.5 (cheapest, fastest)",
        )
    }
}
