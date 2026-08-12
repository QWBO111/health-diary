package com.healthdiary.app.ui.screens.tutor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.TutorIncomeEntity
import com.healthdiary.app.data.local.TutorScheduleEntity
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TutorViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    private val dateFlow = MutableStateFlow(Dates.today())
    var date: String by mutableStateOf(Dates.today())
        private set

    val incomeByDate: StateFlow<List<TutorIncomeEntity>> = dateFlow
            .flatMapLatest { container.tutorRepository.incomeByDate(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIncome: StateFlow<List<TutorIncomeEntity>> =
        container.tutorRepository.allIncome
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedule: StateFlow<List<TutorScheduleEntity>> =
        container.tutorRepository.allSchedule
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeDate(offset: Int) {
        val newDate = runCatching {
            LocalDate.parse(date).plusDays(offset.toLong()).toString()
        }.getOrDefault(Dates.today())
        date = newDate
        dateFlow.value = newDate
    }

    fun addIncome(
        studentName: String,
        subject: String,
        startMinute: Int,
        durationMin: Int,
        income: Float,
        note: String
    ) {
        viewModelScope.launch {
            container.tutorRepository.addIncome(
                date = date,
                studentName = studentName,
                subject = subject,
                startMinute = startMinute,
                durationMin = durationMin,
                income = income,
                note = note
            )
        }
    }

    fun updateIncome(
        id: Long,
        studentName: String,
        subject: String,
        startMinute: Int,
        durationMin: Int,
        income: Float,
        note: String
    ) {
        viewModelScope.launch {
            container.tutorRepository.updateIncome(
                id = id,
                studentName = studentName,
                subject = subject,
                startMinute = startMinute,
                durationMin = durationMin,
                income = income,
                note = note
            )
        }
    }

    fun deleteIncome(id: Long) {
        viewModelScope.launch { container.tutorRepository.deleteIncome(id) }
    }

    fun addSchedule(
        weekday: Int,
        startMinute: Int,
        endMinute: Int,
        studentName: String,
        subject: String,
        note: String,
        fee: Float
    ) {
        viewModelScope.launch {
            container.tutorRepository.addSchedule(
                weekday = weekday,
                startMinute = startMinute,
                endMinute = endMinute,
                studentName = studentName,
                subject = subject,
                note = note,
                fee = fee
            )
        }
    }

    fun updateSchedule(
        id: Long,
        weekday: Int,
        startMinute: Int,
        endMinute: Int,
        studentName: String,
        subject: String,
        note: String,
        fee: Float
    ) {
        viewModelScope.launch {
            container.tutorRepository.updateSchedule(
                id = id,
                weekday = weekday,
                startMinute = startMinute,
                endMinute = endMinute,
                studentName = studentName,
                subject = subject,
                note = note,
                fee = fee
            )
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { container.tutorRepository.deleteSchedule(id) }
    }

    fun checkConflict(
        weekday: Int,
        startMinute: Int,
        endMinute: Int,
        excludeId: Long,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                container.tutorRepository.hasConflict(
                    weekday = weekday,
                    startMinute = startMinute,
                    endMinute = endMinute,
                    excludeId = excludeId
                )
            )
        }
    }
}
