package com.healthdiary.app.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.WorkoutSessionWithDetailsAndSets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDayViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    private val dateFlow = MutableStateFlow("")

    val sessions: StateFlow<List<WorkoutSessionWithDetailsAndSets>> =
        dateFlow
            .flatMapLatest { date ->
                if (date.isBlank()) {
                    flowOf(emptyList())
                } else {
                    container.workoutRepository.allSessionDetails
                        .map { list -> list.filter { it.session.date == date } }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(date: String) {
        dateFlow.value = date
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            container.workoutRepository.getSessionDetails(id)?.let {
                container.workoutRepository.deleteSession(it)
            }
        }
    }
}
