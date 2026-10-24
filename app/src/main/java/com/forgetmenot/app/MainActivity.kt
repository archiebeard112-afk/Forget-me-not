package com.forgetmenot.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgetmenot.app.quickadd.QuickAddActivity
import com.forgetmenot.app.reminders.AlarmBuzzService
import com.forgetmenot.app.ui.EditTaskScreen
import com.forgetmenot.app.ui.ForgetMeNotTheme
import com.forgetmenot.app.ui.MainViewModel
import com.forgetmenot.app.ui.SettingsScreen
import com.forgetmenot.app.ui.TaskListScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private sealed interface Screen {
        data object List : Screen
        data object Settings : Screen
        data class Edit(val taskId: Long) : Screen
    }

    private var screen by mutableStateOf<Screen>(Screen.List)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleIntent(intent)
        setContent {
            ForgetMeNotTheme {
                val tasks by vm.tasks.collectAsStateWithLifecycle()
                when (val s = screen) {
                    Screen.List -> TaskListScreen(
                        tasks = tasks,
                        onOpen = { screen = Screen.Edit(it.id) },
                        onToggleDone = { vm.setDone(it.id, !it.done) },
                        onAdd = { startActivity(Intent(this, QuickAddActivity::class.java)) },
                        onSettings = { screen = Screen.Settings },
                    )
                    Screen.Settings -> {
                        BackHandler { screen = Screen.List }
                        SettingsScreen(vm.prefs) { screen = Screen.List }
                    }
                    is Screen.Edit -> {
                        BackHandler { screen = Screen.List }
                        val task = tasks.firstOrNull { it.id == s.taskId }
                        if (task == null) {
                            // Still loading, or deleted.
                            if (tasks.isNotEmpty()) screen = Screen.List
                        } else {
                            EditTaskScreen(
                                task = task,
                                onSave = { vm.save(it); screen = Screen.List },
                                onToggleDone = { vm.setDone(it.id, !it.done); screen = Screen.List },
                                onDelete = { vm.delete(it.id); screen = Screen.List },
                                onBack = { screen = Screen.List },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        // Opened from a reminder: stop the alarm vibration.
        if (id > 0) AlarmBuzzService.stop(this)
        if (id > 0) screen = Screen.Edit(id)
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}
