package com.forgetmenot.app

import android.app.Application
import android.content.Context
import com.forgetmenot.app.data.AppDatabase
import com.forgetmenot.app.data.Prefs
import com.forgetmenot.app.data.TaskRepository
import com.forgetmenot.app.reminders.Notifications
import com.forgetmenot.app.reminders.ReminderScheduler

class ForgetMeNotApp : Application() {
    lateinit var repository: TaskRepository
        private set
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repository = TaskRepository(AppDatabase.create(this).taskDao(), ReminderScheduler(this))
        Notifications.createChannels(this)
        if (prefs.pinnedQuickAdd) Notifications.showQuickAdd(this)
    }
}

val Context.app: ForgetMeNotApp get() = applicationContext as ForgetMeNotApp
