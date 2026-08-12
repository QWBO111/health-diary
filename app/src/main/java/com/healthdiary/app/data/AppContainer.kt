package com.healthdiary.app.data

import android.content.Context
import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.media.MediaStore
import com.healthdiary.app.data.reminder.ReminderScheduler
import com.healthdiary.app.data.repository.BackupRepository
import com.healthdiary.app.data.repository.BodyRepository
import com.healthdiary.app.data.repository.DiaryRepository
import com.healthdiary.app.data.repository.DietRepository
import com.healthdiary.app.data.repository.TutorRepository
import com.healthdiary.app.data.repository.WorkoutRepository
import com.healthdiary.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    val mediaStore = MediaStore(context)
    val database = AppDatabase.getInstance(context)

    val settingsRepository = SettingsRepository(context)
    val workoutRepository = WorkoutRepository(database, mediaStore)
    val dietRepository = DietRepository(database, mediaStore)
    val bodyRepository = BodyRepository(database, mediaStore)
    val diaryRepository = DiaryRepository(database, mediaStore)
    val tutorRepository = TutorRepository(database)
    val backupRepository = BackupRepository(database, mediaStore)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            com.healthdiary.app.data.local.SeedData.seedIfEmpty(database)
            com.healthdiary.app.data.local.SeedData.ensureFoodLibraryComplete(database)
            if (settingsRepository.reminderEnabled.first()) {
                val (hour, minute) = settingsRepository.reminderTime.first()
                ReminderScheduler.schedule(context, hour, minute)
            }
        }
    }
}
