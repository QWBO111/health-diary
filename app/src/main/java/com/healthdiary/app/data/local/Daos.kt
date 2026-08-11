package com.healthdiary.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert suspend fun insertSession(session: WorkoutSessionEntity): Long
    @Update suspend fun updateSession(session: WorkoutSessionEntity)
    @Query("DELETE FROM workout_sessions WHERE id = :id") suspend fun deleteSession(id: Long)
    @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId") suspend fun deleteExercises(sessionId: Long)
    @Query("DELETE FROM workout_sets WHERE exerciseId IN (SELECT id FROM workout_exercises WHERE sessionId = :sessionId)") suspend fun deleteSets(sessionId: Long)

    @Insert suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long
    @Insert suspend fun insertSets(sets: List<WorkoutSetEntity>)
    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY setNumber")
    suspend fun getSetsForExercise(exerciseId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, startTime DESC") fun observeAllSessions(): Flow<List<WorkoutSessionEntity>>
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY startTime DESC") fun observeSessionsByDate(date: String): Flow<List<WorkoutSessionEntity>>
    @Query("SELECT * FROM workout_exercises") fun observeAllExercises(): Flow<List<WorkoutExerciseEntity>>
    @Query("SELECT * FROM workout_sets") fun observeAllSets(): Flow<List<WorkoutSetEntity>>
    @Query(
        """
        SELECT
            ws.date AS date,
            COUNT(DISTINCT ws.id) AS sessionCount,
            COUNT(wst.id) AS totalSets,
            COALESCE(SUM(wst.reps), 0) AS totalReps,
            COALESCE(SUM(wst.weightKg * wst.reps), 0) AS totalVolumeKg,
            COALESCE((
                SELECT SUM(CASE WHEN s2.endTime IS NOT NULL THEN s2.endTime - s2.startTime ELSE 0 END)
                FROM workout_sessions s2
                WHERE s2.date = ws.date
            ), 0) AS totalDurationMs,
            COUNT(DISTINCT we.exerciseName) AS exerciseCount
        FROM workout_sessions ws
        LEFT JOIN workout_exercises we ON we.sessionId = ws.id
        LEFT JOIN workout_sets wst ON wst.exerciseId = we.id
        GROUP BY ws.date
        ORDER BY ws.date DESC
        """
    ) fun observeDailyStats(): Flow<List<DailyWorkoutStat>>
    @Query(
        """
        SELECT
            ws.date AS date,
            COUNT(DISTINCT ws.id) AS sessionCount,
            COUNT(wst.id) AS totalSets,
            COALESCE(SUM(wst.reps), 0) AS totalReps,
            COALESCE(SUM(wst.weightKg * wst.reps), 0) AS totalVolumeKg,
            COALESCE((
                SELECT SUM(CASE WHEN s2.endTime IS NOT NULL THEN s2.endTime - s2.startTime ELSE 0 END)
                FROM workout_sessions s2
                WHERE s2.date = ws.date
            ), 0) AS totalDurationMs,
            COUNT(DISTINCT we.exerciseName) AS exerciseCount
        FROM workout_sessions ws
        LEFT JOIN workout_exercises we ON we.sessionId = ws.id
        LEFT JOIN workout_sets wst ON wst.exerciseId = we.id
        WHERE ws.date = :date
        GROUP BY ws.date
        """
    ) fun observeDailyStatsByDate(date: String): Flow<DailyWorkoutStat?>
    @Transaction @Query("SELECT * FROM workout_sessions WHERE id = :id") fun observeSessionDetails(id: Long): Flow<WorkoutSessionWithDetails?>
    @Query("SELECT * FROM workout_sessions WHERE id = :id") suspend fun getSession(id: Long): WorkoutSessionEntity?
    @Transaction @Query("SELECT * FROM workout_sessions WHERE id = :id") suspend fun getSessionDetails(id: Long): WorkoutSessionWithDetails?
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY startTime DESC LIMIT 1") suspend fun getLatestSessionOn(date: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions") suspend fun getAllSessions(): List<WorkoutSessionEntity>
    @Query("SELECT * FROM workout_exercises") suspend fun getAllExercises(): List<WorkoutExerciseEntity>
    @Query("SELECT * FROM workout_sets") suspend fun getAllSets(): List<WorkoutSetEntity>
    @Query("DELETE FROM workout_sets") suspend fun clearWorkoutSets()
    @Query("DELETE FROM workout_exercises") suspend fun clearWorkoutExercises()
    @Query("DELETE FROM workout_sessions") suspend fun clearWorkoutSessions()
}

@Dao
interface ExerciseLibraryDao {
    @Insert suspend fun insert(exercise: ExerciseEntity): Long
    @Insert suspend fun insertAll(exercises: List<ExerciseEntity>)
    @Query("SELECT * FROM exercise_library ORDER BY name") fun observeAll(): Flow<List<ExerciseEntity>>
    @Query("SELECT * FROM exercise_library ORDER BY name") suspend fun getAll(): List<ExerciseEntity>
    @Query("SELECT * FROM exercise_library WHERE name LIKE '%' || :query || '%' ORDER BY name") suspend fun search(query: String): List<ExerciseEntity>
    @Query("DELETE FROM exercise_library WHERE id = :id AND isCustom = 1") suspend fun deleteCustom(id: Long)
    @Query("SELECT COUNT(*) FROM exercise_library") suspend fun count(): Int
    @Query("DELETE FROM exercise_library") suspend fun clearExercises()
}

@Dao
interface DietDao {
    @Insert suspend fun insertMeal(meal: MealRecordEntity): Long
    @Update suspend fun updateMeal(meal: MealRecordEntity)
    @Query("DELETE FROM meal_records WHERE id = :id") suspend fun deleteMeal(id: Long)
    @Query("SELECT * FROM meal_records WHERE id = :id") suspend fun getMeal(id: Long): MealRecordEntity?

    @Insert suspend fun insertFood(food: FoodEntryEntity): Long
    @Update suspend fun updateFood(food: FoodEntryEntity)
    @Query("DELETE FROM food_entries WHERE id = :id") suspend fun deleteFood(id: Long)
    @Query("DELETE FROM food_entries WHERE mealId = :mealId") suspend fun deleteFoodsForMeal(mealId: Long)

    @Transaction @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY createdAt") fun observeMeals(date: String): Flow<List<MealRecordWithFood>>
    @Transaction @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY createdAt") suspend fun getMeals(date: String): List<MealRecordWithFood>

    @Query("SELECT * FROM meal_records") suspend fun getAllMeals(): List<MealRecordEntity>
    @Query("SELECT * FROM food_entries") suspend fun getAllFoodEntries(): List<FoodEntryEntity>
    @Query("DELETE FROM food_entries") suspend fun clearFoodEntries()
    @Query("DELETE FROM meal_records") suspend fun clearMealRecords()
}

@Dao
interface FoodLibraryDao {
    @Insert suspend fun insert(food: FoodEntity): Long
    @Insert suspend fun insertAll(foods: List<FoodEntity>)
    @Query("SELECT * FROM food_library ORDER BY name") suspend fun getAll(): List<FoodEntity>
    @Query("SELECT * FROM food_library WHERE name LIKE '%' || :query || '%' ORDER BY name") suspend fun search(query: String): List<FoodEntity>
    @Query("SELECT COUNT(*) FROM food_library") suspend fun count(): Int
    @Query("DELETE FROM food_library") suspend fun clearFoods()
}

@Dao
interface BodyDao {
    @Insert suspend fun insertMetric(metric: BodyMetricEntity): Long
    @Update suspend fun updateMetric(metric: BodyMetricEntity)
    @Query("SELECT * FROM body_metrics WHERE date = :date LIMIT 1") suspend fun getMetricByDate(date: String): BodyMetricEntity?
    @Query("SELECT * FROM body_metrics WHERE date = :date LIMIT 1") fun observeMetricByDate(date: String): Flow<BodyMetricEntity?>
    @Query("SELECT * FROM body_metrics ORDER BY date ASC") fun observeAllMetrics(): Flow<List<BodyMetricEntity>>
    @Query("SELECT * FROM body_metrics ORDER BY date ASC") suspend fun getAllMetrics(): List<BodyMetricEntity>

    @Insert suspend fun insertPhoto(photo: BodyPhotoEntity): Long
    @Query("DELETE FROM body_photos WHERE id = :id") suspend fun deletePhoto(id: Long)
    @Query("SELECT * FROM body_photos WHERE date = :date ORDER BY angle") fun observePhotosByDate(date: String): Flow<List<BodyPhotoEntity>>
    @Query("SELECT * FROM body_photos WHERE date = :date ORDER BY angle") suspend fun getPhotosByDate(date: String): List<BodyPhotoEntity>
    @Query("SELECT * FROM body_photos ORDER BY date ASC, angle") suspend fun getAllPhotos(): List<BodyPhotoEntity>
    @Query("SELECT * FROM body_photos ORDER BY date ASC, angle") fun observeAllPhotos(): Flow<List<BodyPhotoEntity>>

    @Query("DELETE FROM body_photos") suspend fun clearBodyPhotos()
    @Query("DELETE FROM body_metrics") suspend fun clearBodyMetrics()
}

@Dao
interface DiaryDao {
    @Insert suspend fun insertEntry(entry: DiaryEntryEntity): Long
    @Update suspend fun updateEntry(entry: DiaryEntryEntity)
    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1") suspend fun getEntryByDate(date: String): DiaryEntryEntity?
    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1") fun observeEntryByDate(date: String): Flow<DiaryEntryEntity?>
    @Query("SELECT COUNT(*) FROM diary_entries WHERE date = :date") suspend fun countForDate(date: String): Int

    @Insert suspend fun insertMedia(media: DiaryMediaEntity): Long
    @Query("DELETE FROM diary_media WHERE id = :id") suspend fun deleteMedia(id: Long)
    @Query("DELETE FROM diary_media WHERE entryId = :entryId") suspend fun deleteMediaForEntry(entryId: Long)
    @Transaction @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1") fun observeEntryWithMedia(date: String): Flow<DiaryEntryWithMedia?>
    @Transaction @Query("SELECT * FROM diary_entries ORDER BY date DESC") fun observeAllEntries(): Flow<List<DiaryEntryWithMedia>>

    @Query("SELECT * FROM diary_entries") suspend fun getAllEntries(): List<DiaryEntryEntity>
    @Query("SELECT * FROM diary_media") suspend fun getAllMedia(): List<DiaryMediaEntity>
    @Query("DELETE FROM diary_media") suspend fun clearDiaryMedia()
    @Query("DELETE FROM diary_entries") suspend fun clearDiaryEntries()
}
