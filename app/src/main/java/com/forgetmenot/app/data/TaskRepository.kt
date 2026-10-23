package com.forgetmenot.app.data

import com.forgetmenot.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler,
) {
    val tasks: Flow<List<Task>> = dao.observeAll()

    suspend fun get(id: Long): Task? = dao.get(id)

    /** Inserts or updates a task and (re)schedules its reminder. Returns the task id. */
    suspend fun save(task: Task): Long {
        val old = if (task.id != 0L) dao.get(task.id) else null
        val withReminder = task.copy(nextReminderAt = nextReminder(old, task))
        val id = if (old == null) dao.insert(withReminder.copy(id = 0)) else {
            dao.update(withReminder); task.id
        }
        scheduler.sync(withReminder.copy(id = id))
        return id
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val task = dao.get(id) ?: return
        save(task.copy(done = done, completedAt = if (done) System.currentTimeMillis() else null))
    }

    suspend fun snooze(id: Long, until: Long) {
        val task = dao.get(id) ?: return
        val snoozed = task.copy(nextReminderAt = until)
        dao.update(snoozed)
        scheduler.sync(snoozed)
    }

    /** Called after a nag fires: book the next one. */
    suspend fun scheduleNextNag(task: Task, at: Long) {
        val next = task.copy(nextReminderAt = at)
        dao.update(next)
        scheduler.sync(next)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        for (task in dao.pendingReminders()) {
            val at = task.nextReminderAt ?: continue
            val fixed = if (at < now) task.copy(nextReminderAt = now + 30_000) else task
            if (fixed !== task) dao.update(fixed)
            scheduler.sync(fixed)
        }
    }

    private fun nextReminder(old: Task?, new: Task): Long? {
        if (new.done || new.dueAt == null) return null
        val now = System.currentTimeMillis()
        val dueChanged = old == null || old.dueAt != new.dueAt || old.done
        val keep = old?.nextReminderAt
        if (!dueChanged && keep != null) return keep
        // Due in the future: first reminder at the due time. Already due (ASAP): start nagging now.
        return if (new.dueAt > now) new.dueAt else now + 5_000
    }
}
