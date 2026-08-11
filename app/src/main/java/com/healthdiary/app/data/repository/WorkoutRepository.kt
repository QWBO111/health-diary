package com.healthdiary.app.data.repository

import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.local.ExerciseEntity
import com.healthdiary.app.data.local.WorkoutExerciseEntity
import com.healthdiary.app.data.local.WorkoutSessionEntity
import com.healthdiary.app.data.local.WorkoutSessionWithDetails
import com.healthdiary.app.data.local.WorkoutSetEntity
import com.healthdiary.app.data.media.MediaStore
import kotlinx.coroutines.flow.Flow

data class SetDraft(
    val weightKg: Float = 0f,
    val reps: Int = 0,
    val durationSec: Int = 0,
    val restSec: Int = 0,
    val rpe: Int = 0
)

data class ExerciseDraft(
    val exerciseId: Long? = null,
    val name: String,
    val note: String = "",
    val sets: List<SetDraft> = listOf(SetDraft())
)

class WorkoutRepository(
    private val db: AppDatabase,
    @Suppress("unused") private val mediaStore: MediaStore
) {
    val allSessions: Flow<List<WorkoutSessionEntity>> = db.workoutDao().observeAllSessions()

    fun sessionsByDate(date: String): Flow<List<WorkoutSessionEntity>> =
        db.workoutDao().observeSessionsByDate(date)

    fun sessionDetails(id: Long): Flow<WorkoutSessionWithDetails?> =
        db.workoutDao().observeSessionDetails(id)

    suspend fun getSession(id: Long): WorkoutSessionEntity? = db.workoutDao().getSession(id)

    suspend fun getSessionDetails(id: Long): WorkoutSessionWithDetails? =
        db.workoutDao().getSessionDetails(id)

    suspend fun getSetsForExercise(exerciseId: Long): List<WorkoutSetEntity> =
        db.workoutDao().getSetsForExercise(exerciseId)

    suspend fun latestSessionOn(date: String): WorkoutSessionEntity? =
        db.workoutDao().getLatestSessionOn(date)

    suspend fun saveSession(
        existingId: Long?,
        date: String,
        startTime: Long,
        endTime: Long,
        note: String,
        drafts: List<ExerciseDraft>
    ): Long {
        val sessionId = if (existingId != null) {
            db.workoutDao().deleteSets(existingId)
            db.workoutDao().deleteExercises(existingId)
            val session = db.workoutDao().getSession(existingId)
                ?.copy(date = date, startTime = startTime, endTime = endTime, note = note)
                ?: WorkoutSessionEntity(date = date, startTime = startTime, endTime = endTime, note = note)
            db.workoutDao().updateSession(session)
            existingId
        } else {
            db.workoutDao().insertSession(
                WorkoutSessionEntity(date = date, startTime = startTime, endTime = endTime, note = note)
            )
        }

        drafts.forEachIndexed { index, draft ->
            val exId = db.workoutDao().insertExercise(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = draft.exerciseId,
                    exerciseName = draft.name,
                    orderIndex = index,
                    note = draft.note
                )
            )
            draft.sets.forEachIndexed { setIndex, set ->
                db.workoutDao().insertSets(
                    listOf(
                        WorkoutSetEntity(
                            exerciseId = exId,
                            setNumber = setIndex + 1,
                            weightKg = set.weightKg,
                            reps = set.reps,
                            durationSec = set.durationSec,
                            restSec = set.restSec,
                            rpe = set.rpe
                        )
                    )
                )
            }
        }
        return sessionId
    }

    suspend fun deleteSession(session: WorkoutSessionWithDetails) {
        db.workoutDao().deleteSets(session.session.id)
        db.workoutDao().deleteExercises(session.session.id)
        db.workoutDao().deleteSession(session.session.id)
    }

    // ---------- 动作库 ----------

    val exerciseLibrary: Flow<List<ExerciseEntity>> = db.exerciseLibraryDao().observeAll()

    suspend fun searchExercises(query: String): List<ExerciseEntity> =
        db.exerciseLibraryDao().search(query)

    suspend fun addCustomExercise(name: String): Long =
        db.exerciseLibraryDao().insert(ExerciseEntity(name = name, isCustom = true))
}
