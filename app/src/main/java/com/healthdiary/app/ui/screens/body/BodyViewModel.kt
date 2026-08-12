package com.healthdiary.app.ui.screens.body

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.BodyMetricEntity
import com.healthdiary.app.data.local.BodyPhotoEntity
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class BodyViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    var date: String by mutableStateOf(Dates.today())
        private set

    val metric: StateFlow<BodyMetricEntity?> =
        container.bodyRepository.metricByDate(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val photos: StateFlow<List<BodyPhotoEntity>> =
        container.bodyRepository.photosByDate(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMetrics: StateFlow<List<BodyMetricEntity>> =
        container.bodyRepository.allMetrics
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPhotos: StateFlow<List<BodyPhotoEntity>> =
        container.bodyRepository.allPhotos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heightCm: StateFlow<Int> =
        container.settingsRepository.heightCm
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 175)

    fun changeDate(offset: Int) {
        date = runCatching {
            LocalDate.parse(date).plusDays(offset.toLong()).toString()
        }.getOrDefault(Dates.today())
    }

    fun saveWeight(weightText: String) {
        val weight = weightText.toFloatOrNull() ?: return
        viewModelScope.launch { container.bodyRepository.saveWeight(date, weight) }
    }

    fun saveMeasurements(chest: String?, waist: String?, hip: String?) {
        viewModelScope.launch {
            container.bodyRepository.saveMeasurements(
                date,
                chest?.toFloatOrNull(),
                waist?.toFloatOrNull(),
                hip?.toFloatOrNull()
            )
        }
    }

    fun addPhoto(angle: String, uri: Uri) {
        viewModelScope.launch { container.bodyRepository.addPhoto(date, angle, uri) }
    }

    fun deletePhoto(photo: BodyPhotoEntity) {
        viewModelScope.launch { container.bodyRepository.deletePhoto(photo) }
    }
}
