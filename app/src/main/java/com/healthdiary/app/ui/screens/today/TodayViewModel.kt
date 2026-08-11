package com.healthdiary.app.ui.screens.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.BodyMetricEntity
import com.healthdiary.app.data.local.DiaryEntryEntity
import com.healthdiary.app.data.local.MealRecordWithFood
import com.healthdiary.app.data.local.WorkoutSessionEntity
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container
    val today: String = Dates.today()

    val workoutsToday: StateFlow<List<WorkoutSessionEntity>> =
        container.workoutRepository.sessionsByDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealsToday: StateFlow<List<MealRecordWithFood>> =
        container.dietRepository.mealsByDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightToday: StateFlow<BodyMetricEntity?> =
        container.bodyRepository.metricByDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val diaryToday: StateFlow<DiaryEntryEntity?> =
        container.diaryRepository.entryByDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestWeight: StateFlow<BodyMetricEntity?> =
        container.bodyRepository.allMetrics
            .map { it.lastOrNull { m -> m.weightKg != null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
