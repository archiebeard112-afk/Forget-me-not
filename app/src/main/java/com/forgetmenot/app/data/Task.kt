package com.forgetmenot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Who asked, e.g. "Autumn Larson". */
    val requester: String? = null,
    /** Key specifics copied exactly, e.g. "Zents · 5199g · EV25.OBXD.325 · 2-gal bucket". */
    val details: String? = null,
    val notes: String? = null,
    /** Where it came from, e.g. "Teams screenshot". */
    val source: String? = null,
    val dueAt: Long? = null,
    val highPriority: Boolean = false,
    /** Minutes between nags once the task is due. */
    val nagMinutes: Int = 60,
    /** When the next reminder fires; null means no reminder pending. */
    val nextReminderAt: Long? = null,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
