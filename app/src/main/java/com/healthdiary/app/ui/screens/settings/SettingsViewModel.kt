package com.healthdiary.app.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    val reminderEnabled: StateFlow<Boolean> =
        container.settingsRepository.reminderEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderTime: StateFlow<Pair<Int, Int>> =
        container.settingsRepository.reminderTime
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21 to 0)

    val heightCm: StateFlow<Int> =
        container.settingsRepository.heightCm
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 175)

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setReminderEnabled(enabled)
            applyReminderSchedule()
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            container.settingsRepository.setReminderTime(hour, minute)
            applyReminderSchedule()
        }
    }

    fun setHeightCm(cm: Int) {
        viewModelScope.launch { container.settingsRepository.setHeightCm(cm) }
    }

    private suspend fun applyReminderSchedule() {
        val enabled = container.settingsRepository.reminderEnabled.first()
        if (enabled) {
            val (hour, minute) = container.settingsRepository.reminderTime.first()
            ReminderScheduler.schedule(getApplication(), hour, minute)
        } else {
            ReminderScheduler.cancel(getApplication())
        }
    }

    fun exportBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val result = container.backupRepository.exportTo(getApplication(), uri)
                "导出成功：${result.recordCount} 条记录、${result.mediaCount} 个媒体文件"
            }.onSuccess(onResult).onFailure { onResult("导出失败：${it.message}") }
        }
    }

    fun importBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val result = container.backupRepository.importFrom(getApplication(), uri)
                "导入成功：${result.recordCount} 条记录、${result.mediaCount} 个媒体文件"
            }.onSuccess(onResult).onFailure { onResult("导入失败：${it.message}") }
        }
    }
}
