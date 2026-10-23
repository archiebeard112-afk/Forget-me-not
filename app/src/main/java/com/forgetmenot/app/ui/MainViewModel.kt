package com.forgetmenot.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forgetmenot.app.data.Task
import com.forgetmenot.app.ForgetMeNotApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as ForgetMeNotApp).repository
    val prefs = (application as ForgetMeNotApp).prefs

    val tasks: StateFlow<List<Task>> =
        repo.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(task: Task) = viewModelScope.launch { repo.save(task) }
    fun setDone(id: Long, done: Boolean) = viewModelScope.launch { repo.setDone(id, done) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}
