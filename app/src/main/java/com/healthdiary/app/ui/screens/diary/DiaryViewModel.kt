package com.healthdiary.app.ui.screens.diary

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.DiaryEntryWithMedia
import com.healthdiary.app.data.local.DiaryMediaEntity
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    private val dateFlow = MutableStateFlow(Dates.today())
    var date: String by mutableStateOf(Dates.today())
        private set
    var mood: String by mutableStateOf("")
    var moodScore: Int by mutableStateOf(0)
    var text: String by mutableStateOf("")

    val entry: StateFlow<DiaryEntryWithMedia?> = dateFlow
            .flatMapLatest { container.diaryRepository.entryWithMedia(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun changeDate(offset: Int) {
        val newDate = runCatching {
            LocalDate.parse(date).plusDays(offset.toLong()).toString()
        }.getOrDefault(Dates.today())
        date = newDate
        dateFlow.value = newDate
        viewModelScope.launch {
            val e = container.diaryRepository.getEntry(newDate)
            mood = e?.mood ?: ""
            moodScore = e?.moodScore ?: 0
            text = e?.text ?: ""
        }
    }

    fun setMood(score: Int) {
        moodScore = score
        mood = when (score) {
            1 -> "😞"
            2 -> "😕"
            3 -> "😐"
            4 -> "🙂"
            5 -> "😄"
            else -> ""
        }
    }

    fun saveEntry(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            container.diaryRepository.saveEntry(date, mood, moodScore, text)
            onSaved()
        }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            val entryId = getOrCreateEntryId()
            container.diaryRepository.addPhoto(entryId, uri)
        }
    }

    fun addAudio(path: String, durationSec: Int) {
        viewModelScope.launch {
            val entryId = getOrCreateEntryId()
            container.diaryRepository.addAudio(entryId, path, durationSec)
        }
    }

    fun deleteMedia(media: DiaryMediaEntity) {
        viewModelScope.launch { container.diaryRepository.deleteMedia(media) }
    }

    private suspend fun getOrCreateEntryId(): Long =
        container.diaryRepository.getEntry(date)?.id
            ?: container.diaryRepository.saveEntry(date, mood, moodScore, text)
}
