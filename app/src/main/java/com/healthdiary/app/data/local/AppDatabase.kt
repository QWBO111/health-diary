package com.healthdiary.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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
        DiaryMediaEntity::class,
        TutorIncomeEntity::class,
        TutorScheduleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun dietDao(): DietDao
    abstract fun foodLibraryDao(): FoodLibraryDao
    abstract fun bodyDao(): BodyDao
    abstract fun diaryDao(): DiaryDao
    abstract fun tutorDao(): TutorDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tutor_income_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `studentName` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `startMinute` INTEGER NOT NULL,
                        `durationMin` INTEGER NOT NULL,
                        `income` REAL NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tutor_schedule_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `weekday` INTEGER NOT NULL,
                        `startMinute` INTEGER NOT NULL,
                        `endMinute` INTEGER NOT NULL,
                        `studentName` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `tutor_schedule_items` ADD COLUMN `fee` REAL NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_diary.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
