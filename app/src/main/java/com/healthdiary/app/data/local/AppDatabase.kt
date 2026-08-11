package com.healthdiary.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ExerciseEntity::class,
        MealRecordEntity::class,
        FoodEntryEntity::class,
        FoodEntity::class,
        BodyMetricEntity::class,
        BodyPhotoEntity::class,
        DiaryEntryEntity::class,
        DiaryMediaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun dietDao(): DietDao
    abstract fun foodLibraryDao(): FoodLibraryDao
    abstract fun bodyDao(): BodyDao
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_diary.db"
                ).build().also { instance = it }
            }
    }
}
