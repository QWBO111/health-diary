package com.healthdiary.app.data.repository

import android.net.Uri
import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.local.BodyMetricEntity
import com.healthdiary.app.data.local.BodyPhotoEntity
import com.healthdiary.app.data.media.MediaStore
import kotlinx.coroutines.flow.Flow

class BodyRepository(
    private val db: AppDatabase,
    private val mediaStore: MediaStore
) {
    val allMetrics: Flow<List<BodyMetricEntity>> = db.bodyDao().observeAllMetrics()

    fun metricByDate(date: String): Flow<BodyMetricEntity?> =
        db.bodyDao().observeMetricByDate(date)

    suspend fun getMetricByDate(date: String): BodyMetricEntity? =
        db.bodyDao().getMetricByDate(date)

    fun photosByDate(date: String): Flow<List<BodyPhotoEntity>> =
        db.bodyDao().observePhotosByDate(date)

    val allPhotos: Flow<List<BodyPhotoEntity>> = db.bodyDao().observeAllPhotos()

    suspend fun saveWeight(date: String, weightKg: Float) {
        val existing = db.bodyDao().getMetricByDate(date)
        if (existing == null) {
            db.bodyDao().insertMetric(BodyMetricEntity(date = date, weightKg = weightKg))
        } else {
            db.bodyDao().updateMetric(existing.copy(weightKg = weightKg))
        }
    }

    suspend fun saveMeasurements(date: String, chestCm: Float?, waistCm: Float?, hipCm: Float?) {
        val existing = db.bodyDao().getMetricByDate(date)
        if (existing == null) {
            db.bodyDao().insertMetric(
                BodyMetricEntity(date = date, chestCm = chestCm, waistCm = waistCm, hipCm = hipCm)
            )
        } else {
            db.bodyDao().updateMetric(existing.copy(chestCm = chestCm, waistCm = waistCm, hipCm = hipCm))
        }
    }

    suspend fun addPhoto(date: String, angle: String, uri: Uri): String {
        val path = mediaStore.copyPhotoFromUri(uri, "body/$date")
        db.bodyDao().insertPhoto(BodyPhotoEntity(date = date, angle = angle, filePath = path))
        return path
    }

    suspend fun deletePhoto(photo: BodyPhotoEntity) {
        mediaStore.delete(photo.filePath)
        db.bodyDao().deletePhoto(photo.id)
    }

    suspend fun getPhotosByDate(date: String): List<BodyPhotoEntity> =
        db.bodyDao().getPhotosByDate(date)
}
