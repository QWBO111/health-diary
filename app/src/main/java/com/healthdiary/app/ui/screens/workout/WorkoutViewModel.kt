package com.healthdiary.app.ui.screens.workout

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.DailyWorkoutStat
import com.healthdiary.app.data.local.ExerciseEntity
import com.healthdiary.app.data.repository.ExerciseDraft
import com.healthdiary.app.data.repository.SetDraft
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    val dailyStats: StateFlow<List<DailyWorkoutStat>> =
        container.workoutRepository.dailyStats
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyStats: StateFlow<List<DailyWorkoutStat>> =
        container.workoutRepository.dailyStats
            .map { stats ->
                val byDate = stats.associateBy { it.date }
                val today = LocalDate.now()
                (0..6).map { offset ->
                    val date = today.minusDays(6L - offset).toString()
                    byDate[date] ?: DailyWorkoutStat(
                        date = date,
                        sessionCount = 0,
                        totalSets = 0,
                        totalReps = 0,
                        totalVolumeKg = 0f,
                        totalDurationMs = 0L,
                        exerciseCount = 0
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            container.workoutRepository.getSessionDetails(id)?.let {
                container.workoutRepository.deleteSession(it)
            }
        }
    }
}

class WorkoutEditViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    val library: StateFlow<List<ExerciseEntity>> =
        container.workoutRepository.exerciseLibrary
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var sessionId: Long = -1
        private set
    var loading: Boolean by mutableStateOf(true)
    var selectedDate: String by mutableStateOf(Dates.today())
    var note: String by mutableStateOf("")
    var startTime: Long by mutableStateOf(System.currentTimeMillis())
    var drafts: List<ExerciseDraft> by mutableStateOf(emptyList())

    fun init(sessionId: Long) {
        this.sessionId = sessionId
        viewModelScope.launch {
            if (sessionId >= 0) {
                val details = container.workoutRepository.getSessionDetails(sessionId)
                if (details != null) {
                    selectedDate = details.session.date
                    note = details.session.note
                    startTime = details.session.startTime
                    drafts = details.exercises
                        .sortedBy { it.orderIndex }
                        .map { ex ->
                            val sets = container.workoutRepository.getSetsForExercise(ex.id)
                                .sortedBy { it.setNumber }
                                .map {
                                    SetDraft(
                                        weightKg = it.weightKg,
                                        reps = it.reps,
                                        durationSec = it.durationSec,
                                        restSec = it.restSec,
                                        rpe = it.rpe
                                    )
                                }
                            ExerciseDraft(
                                exerciseId = ex.exerciseId,
                                name = ex.exerciseName,
                                note = ex.note,
                                sets = sets.ifEmpty { listOf(SetDraft()) }
                            )
                        }
                }
            }
            loading = false
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        if (drafts.none { it.exerciseId == exercise.id }) {
            drafts = drafts + ExerciseDraft(exerciseId = exercise.id, name = exercise.name)
        }
    }

    fun addCustomExercise(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = container.workoutRepository.addCustomExercise(name.trim())
            drafts = drafts + ExerciseDraft(exerciseId = id, name = name.trim())
        }
    }

    fun removeExercise(index: Int) {
        drafts = drafts.filterIndexed { i, _ -> i != index }
    }

    fun removeExerciseById(exerciseId: Long) {
        drafts = drafts.filter { it.exerciseId != exerciseId }
    }

    fun addSet(exerciseIndex: Int) {
        drafts = drafts.mapIndexed { i, d ->
            if (i == exerciseIndex) d.copy(sets = d.sets + SetDraft()) else d
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        drafts = drafts.mapIndexed { i, d ->
            if (i == exerciseIndex) d.copy(sets = d.sets.filterIndexed { si, _ -> si != setIndex }) else d
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, field: SetField, value: String) {
        drafts = drafts.mapIndexed { i, d ->
            if (i != exerciseIndex) return@mapIndexed d
            d.copy(
                sets = d.sets.mapIndexed { si, s ->
                    if (si != setIndex) s else {
                        when (field) {
                            SetField.WEIGHT -> s.copy(weightKg = value.toFloatOrNull() ?: 0f)
                            SetField.REPS -> s.copy(reps = value.toIntOrNull() ?: 0)
                            SetField.DURATION -> s.copy(durationSec = value.toIntOrNull() ?: 0)
                        }
                    }
                }
            )
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            container.workoutRepository.saveSession(
                existingId = if (sessionId >= 0) sessionId else null,
                date = selectedDate,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                note = note,
                drafts = drafts
            )
            onDone()
        }
    }
}

enum class SetField { WEIGHT, REPS, DURATION }
