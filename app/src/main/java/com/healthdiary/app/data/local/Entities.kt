package com.healthdiary.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

// ---------- 训练 ----------

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,               // yyyy-MM-dd
    val startTime: Long,
    val endTime: Long? = null,
    val note: String = ""
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long? = null,   // 引用动作库；自定义动作时为 null
    val exerciseName: String,
    val orderIndex: Int = 0,
    val note: String = ""
)

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Float = 0f,
    val reps: Int = 0,
    val durationSec: Int = 0,
    val restSec: Int = 0,
    val rpe: Int = 0
)

@Entity(tableName = "exercise_library")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String = "",
    val isCustom: Boolean = false
)

data class WorkoutSessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<WorkoutExerciseEntity>
)

data class WorkoutExerciseWithSets(
    @Embedded val exercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val sets: List<WorkoutSetEntity>
)

data class WorkoutSessionWithDetailsAndSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<WorkoutExerciseWithSets>
)

data class DailyWorkoutStat(
    val date: String,
    val sessionCount: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Float,
    val totalDurationMs: Long,
    val exerciseCount: Int
)

// ---------- 饮食 ----------

@Entity(tableName = "meal_records")
data class MealRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val mealType: String,           // 早餐/午餐/晚餐/加餐
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val name: String,
    val grams: Float = 0f,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f
)

@Entity(tableName = "food_library")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesPer100g: Float = 0f,
    val proteinPer100g: Float = 0f,
    val carbsPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val category: String = ""
)

data class MealRecordWithFood(
    @Embedded val meal: MealRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "mealId")
    val foods: List<FoodEntryEntity>
)

// ---------- 身体 ----------

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Float? = null,
    val chestCm: Float? = null,
    val waistCm: Float? = null,
    val hipCm: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_photos")
data class BodyPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val angle: String,              // 正面/侧面/背面
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class BodyMetricWithPhotos(
    @Embedded val metric: BodyMetricEntity,
    @Relation(parentColumn = "date", entityColumn = "date")
    val photos: List<BodyPhotoEntity>
)

// ---------- 日记 ----------

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val mood: String = "",
    val moodScore: Int = 0,         // 1-5
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "diary_media")
data class DiaryMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val type: String,               // photo / audio
    val filePath: String,
    val durationSec: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class DiaryEntryWithMedia(
    @Embedded val entry: DiaryEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val media: List<DiaryMediaEntity>
)

// ---------- 家教 ----------

@Entity(tableName = "tutor_income_records")
data class TutorIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,               // yyyy-MM-dd
    val studentName: String = "",
    val subject: String = "",
    val startMinute: Int = 0,       // 0-1439，上课开始时间
    val durationMin: Int = 60,
    val income: Float = 0f,         // 元
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tutor_schedule_items")
data class TutorScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekday: Int,               // 1=周一 ... 7=周日
    val startMinute: Int = 0,       // 0-1439
    val endMinute: Int = 60,
    val studentName: String = "",
    val subject: String = "",
    val note: String = "",
    val fee: Float = 0f              // 每节课费（元）
)
