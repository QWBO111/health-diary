package com.healthdiary.app.data.repository

import android.net.Uri
import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.local.DiaryEntryEntity
import com.healthdiary.app.data.local.DiaryEntryWithMedia
import com.healthdiary.app.data.local.DiaryMediaEntity
import com.healthdiary.app.data.media.MediaStore
import kotlinx.coroutines.flow.Flow

class DiaryRepository(
    private val db: AppDatabase,
    private val mediaStore: MediaStore
) {
    fun entryByDate(date: String): Flow<DiaryEntryEntity?> =
        db.diaryDao().observeEntryByDate(date)

    fun entryWithMedia(date: String): Flow<DiaryEntryWithMedia?> =
        db.diaryDao().observeEntryWithMedia(date)

    fun allEntries(): Flow<List<DiaryEntryWithMedia>> =
        db.diaryDao().observeAllEntries()

    suspend fun getEntry(date: String): DiaryEntryEntity? =
        db.diaryDao().getEntryByDate(date)

    suspend fun saveEntry(date: String, mood: String, moodScore: Int, text: String): Long {
        val existing = db.diaryDao().getEntryByDate(date)
        val now = System.currentTimeMillis()
        return if (existing == null) {
            db.diaryDao().insertEntry(
                DiaryEntryEntity(date = date, mood = mood, moodScore = moodScore, text = text, createdAt = now, updatedAt = now)
            )
        } else {
            db.diaryDao().updateEntry(existing.copy(mood = mood, moodScore = moodScore, text = text, updatedAt = now))
            existing.id
        }
    }

    suspend fun addPhoto(entryId: Long, uri: Uri): String {
        val path = mediaStore.copyPhotoFromUri(uri, "diary")
        db.diaryDao().insertMedia(
            DiaryMediaEntity(entryId = entryId, type = "photo", filePath = path)
        )
        return path
    }

    suspend fun addAudio(entryId: Long, path: String, durationSec: Int) {
        db.diaryDao().insertMedia(
            DiaryMediaEntity(entryId = entryId, type = "audio", filePath = path, durationSec = durationSec)
        )
    }

    suspend fun deleteMedia(media: DiaryMediaEntity) {
        mediaStore.delete(media.filePath)
        db.diaryDao().deleteMedia(media.id)
    }

    suspend fun hasEntry(date: String): Boolean =
        db.diaryDao().countForDate(date) > 0
}
