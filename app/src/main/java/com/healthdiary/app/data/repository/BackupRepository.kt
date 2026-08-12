package com.healthdiary.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.local.BodyMetricEntity
import com.healthdiary.app.data.local.BodyPhotoEntity
import com.healthdiary.app.data.local.DiaryEntryEntity
import com.healthdiary.app.data.local.DiaryMediaEntity
import com.healthdiary.app.data.local.ExerciseEntity
import com.healthdiary.app.data.local.FoodEntity
import com.healthdiary.app.data.local.FoodEntryEntity
import com.healthdiary.app.data.local.MealRecordEntity
import com.healthdiary.app.data.local.TutorIncomeEntity
import com.healthdiary.app.data.local.TutorScheduleEntity
import com.healthdiary.app.data.local.WorkoutExerciseEntity
import com.healthdiary.app.data.local.WorkoutSessionEntity
import com.healthdiary.app.data.local.WorkoutSetEntity
import com.healthdiary.app.data.media.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val db: AppDatabase,
    private val mediaStore: MediaStore
) {

    data class BackupResult(
        val recordCount: Int,
        val mediaCount: Int
    )

    suspend fun exportTo(context: Context, uri: Uri): BackupResult {
        val json = buildJson()
        val files = collectMediaFiles()
        val zos = ZipOutputStream(
            context.contentResolver.openOutputStream(uri) ?: error("无法创建备份文件")
        )
        zos.use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val added = mutableSetOf<String>()
            files.forEach { file ->
                val key = mediaStore.relativeKey(file.absolutePath)
                if (file.isFile && added.add(key)) {
                    zip.putNextEntry(ZipEntry(key))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return BackupResult(recordCount = json.length() - 3, mediaCount = files.size)
    }

    suspend fun importFrom(context: Context, uri: Uri): BackupResult {
        val zipInput = ZipInputStream(
            context.contentResolver.openInputStream(uri) ?: error("无法读取备份文件")
        )
        val tempDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
        var json: JSONObject? = null

        zipInput.use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "data.json") {
                    json = JSONObject(zis.readBytes().toString(Charsets.UTF_8))
                } else if (entry.name.startsWith("media/")) {
                    val out = File(tempDir, entry.name.removePrefix("media/"))
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val data = json ?: error("备份文件缺少 data.json")
        try {
            val existingPaths = mediaStore.allMediaFiles().map { it.absolutePath }.toHashSet()
            val mediaMap = restoreMediaFiles(tempDir, data)
            val newFiles = mediaStore.allMediaFiles().filter { it.absolutePath !in existingPaths }

            try {
                db.withTransaction {
                    clearAll()
                    restoreWorkout(data)
                    restoreDiet(data, mediaMap)
                    restoreBody(data, mediaMap)
                    restoreDiary(data, mediaMap)
                    restoreTutor(data)
                }
            } catch (e: Exception) {
                newFiles.forEach { it.delete() }
                throw e
            }

            val kept = mediaMap.values.toHashSet()
            existingPaths.forEach { path ->
                if (path !in kept) File(path).delete()
            }

            return BackupResult(
                recordCount = listOf(
                    "workoutSessions", "workoutExercises", "workoutSets", "exerciseLibrary",
                    "mealRecords", "foodEntries", "foodLibrary",
                    "bodyMetrics", "bodyPhotos",
                    "diaryEntries", "diaryMedia",
                    "tutorIncome", "tutorSchedule"
                ).sumOf { data.optJSONArray(it)?.length() ?: 0 },
                mediaCount = mediaMap.size
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ---------- 导出辅助 ----------

    private suspend fun buildJson(): JSONObject {
        val root = JSONObject()
        root.put("app", "闲记")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("workoutSessions", JSONArray().apply {
            db.workoutDao().getAllSessions().forEach { put(it.toJson()) }
        })
        root.put("workoutExercises", JSONArray().apply {
            db.workoutDao().getAllExercises().forEach { put(it.toJson()) }
        })
        root.put("workoutSets", JSONArray().apply {
            db.workoutDao().getAllSets().forEach { put(it.toJson()) }
        })
        root.put("exerciseLibrary", JSONArray().apply {
            db.exerciseLibraryDao().getAll().forEach { put(it.toJson()) }
        })
        root.put("mealRecords", JSONArray().apply {
            db.dietDao().getAllMeals().forEach { put(it.toJson()) }
        })
        root.put("foodEntries", JSONArray().apply {
            db.dietDao().getAllFoodEntries().forEach { put(it.toJson()) }
        })
        root.put("foodLibrary", JSONArray().apply {
            db.foodLibraryDao().getAll().forEach { put(it.toJson()) }
        })
        root.put("bodyMetrics", JSONArray().apply {
            db.bodyDao().getAllMetrics().forEach { put(it.toJson()) }
        })
        root.put("bodyPhotos", JSONArray().apply {
            db.bodyDao().getAllPhotos().forEach { put(it.toJson()) }
        })
        root.put("diaryEntries", JSONArray().apply {
            db.diaryDao().getAllEntries().forEach { put(it.toJson()) }
        })
        root.put("diaryMedia", JSONArray().apply {
            db.diaryDao().getAllMedia().forEach { put(it.toJson()) }
        })
        root.put("tutorIncome", JSONArray().apply {
            db.tutorDao().getAllIncome().forEach { put(it.toJson()) }
        })
        root.put("tutorSchedule", JSONArray().apply {
            db.tutorDao().getAllSchedule().forEach { put(it.toJson()) }
        })
        return root
    }

    private fun collectMediaFiles(): List<File> =
        mediaStore.allMediaFiles().filter { it.isFile }

    // ---------- 导入辅助 ----------

    private fun restoreMediaFiles(tempDir: File, data: JSONObject): Map<String, String> {
        val keys = mutableSetOf<String>()
        forEachJson(data, "bodyPhotos") { keys.add(it.optString("filePath")) }
        forEachJson(data, "mealRecords") {
            val p = it.optString("photoPath")
            if (p.isNotBlank()) keys.add(p)
        }
        forEachJson(data, "diaryMedia") { keys.add(it.optString("filePath")) }

        val map = mutableMapOf<String, String>()
        keys.filter { it.startsWith("media/") }.forEach { key ->
            val src = File(tempDir, key.removePrefix("media/"))
            if (src.isFile) {
                val dest = File(mediaStore.mediaRoot, key.removePrefix("media/"))
                dest.parentFile?.mkdirs()
                src.copyTo(dest, overwrite = true)
                map[key] = dest.absolutePath
            }
        }
        return map
    }

    private suspend fun clearAll() {
        db.diaryDao().clearDiaryMedia()
        db.diaryDao().clearDiaryEntries()
        db.dietDao().clearFoodEntries()
        db.dietDao().clearMealRecords()
        db.bodyDao().clearBodyPhotos()
        db.bodyDao().clearBodyMetrics()
        db.workoutDao().clearWorkoutSets()
        db.workoutDao().clearWorkoutExercises()
        db.workoutDao().clearWorkoutSessions()
        db.exerciseLibraryDao().clearExercises()
        db.foodLibraryDao().clearFoods()
        db.tutorDao().clearIncome()
        db.tutorDao().clearSchedule()
    }

    private suspend fun restoreWorkout(data: JSONObject) {
        forEachJson(data, "exerciseLibrary") {
            db.exerciseLibraryDao().insert(
                ExerciseEntity(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    muscleGroup = it.optString("muscleGroup"),
                    isCustom = it.optBoolean("isCustom")
                )
            )
        }
        forEachJson(data, "workoutSessions") {
            db.workoutDao().insertSession(
                WorkoutSessionEntity(
                    id = it.getLong("id"),
                    date = it.getString("date"),
                    startTime = it.getLong("startTime"),
                    endTime = if (it.isNull("endTime")) null else it.getLong("endTime"),
                    note = it.optString("note")
                )
            )
        }
        forEachJson(data, "workoutExercises") {
            db.workoutDao().insertExercise(
                WorkoutExerciseEntity(
                    id = it.getLong("id"),
                    sessionId = it.getLong("sessionId"),
                    exerciseId = if (it.isNull("exerciseId")) null else it.getLong("exerciseId"),
                    exerciseName = it.getString("exerciseName"),
                    orderIndex = it.optInt("orderIndex"),
                    note = it.optString("note")
                )
            )
        }
        forEachJson(data, "workoutSets") {
            db.workoutDao().insertSets(
                listOf(
                    WorkoutSetEntity(
                        id = it.getLong("id"),
                        exerciseId = it.getLong("exerciseId"),
                        setNumber = it.getInt("setNumber"),
                        weightKg = it.optDouble("weightKg").toFloat(),
                        reps = it.optInt("reps"),
                        durationSec = it.optInt("durationSec"),
                        restSec = it.optInt("restSec"),
                        rpe = it.optInt("rpe")
                    )
                )
            )
        }
    }

    private suspend fun restoreDiet(data: JSONObject, mediaMap: Map<String, String>) {
        forEachJson(data, "foodLibrary") {
            db.foodLibraryDao().insert(
                FoodEntity(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    caloriesPer100g = it.optDouble("caloriesPer100g").toFloat(),
                    proteinPer100g = it.optDouble("proteinPer100g").toFloat(),
                    carbsPer100g = it.optDouble("carbsPer100g").toFloat(),
                    fatPer100g = it.optDouble("fatPer100g").toFloat(),
                    category = it.optString("category")
                )
            )
        }
        forEachJson(data, "mealRecords") { o ->
            val photoKey = o.optString("photoPath")
            db.dietDao().insertMeal(
                MealRecordEntity(
                    id = o.getLong("id"),
                    date = o.getString("date"),
                    mealType = o.getString("mealType"),
                    photoPath = if (photoKey.isBlank()) null else mediaMap[photoKey],
                    createdAt = o.optLong("createdAt")
                )
            )
        }
        forEachJson(data, "foodEntries") {
            db.dietDao().insertFood(
                FoodEntryEntity(
                    id = it.getLong("id"),
                    mealId = it.getLong("mealId"),
                    name = it.getString("name"),
                    grams = it.optDouble("grams").toFloat(),
                    calories = it.optDouble("calories").toFloat(),
                    protein = it.optDouble("protein").toFloat(),
                    carbs = it.optDouble("carbs").toFloat(),
                    fat = it.optDouble("fat").toFloat()
                )
            )
        }
    }

    private suspend fun restoreBody(data: JSONObject, mediaMap: Map<String, String>) {
        forEachJson(data, "bodyMetrics") {
            db.bodyDao().insertMetric(
                BodyMetricEntity(
                    id = it.getLong("id"),
                    date = it.getString("date"),
                    weightKg = if (it.isNull("weightKg")) null else it.optDouble("weightKg").toFloat(),
                    chestCm = if (it.isNull("chestCm")) null else it.optDouble("chestCm").toFloat(),
                    waistCm = if (it.isNull("waistCm")) null else it.optDouble("waistCm").toFloat(),
                    hipCm = if (it.isNull("hipCm")) null else it.optDouble("hipCm").toFloat(),
                    createdAt = it.optLong("createdAt")
                )
            )
        }
        forEachJson(data, "bodyPhotos") {
            db.bodyDao().insertPhoto(
                BodyPhotoEntity(
                    id = it.getLong("id"),
                    date = it.getString("date"),
                    angle = it.getString("angle"),
                    filePath = mediaMap[it.optString("filePath")] ?: "",
                    createdAt = it.optLong("createdAt")
                )
            )
        }
    }

    private suspend fun restoreDiary(data: JSONObject, mediaMap: Map<String, String>) {
        forEachJson(data, "diaryEntries") {
            db.diaryDao().insertEntry(
                DiaryEntryEntity(
                    id = it.getLong("id"),
                    date = it.getString("date"),
                    mood = it.optString("mood"),
                    moodScore = it.optInt("moodScore"),
                    text = it.optString("text"),
                    createdAt = it.optLong("createdAt"),
                    updatedAt = it.optLong("updatedAt")
                )
            )
        }
        forEachJson(data, "diaryMedia") {
            db.diaryDao().insertMedia(
                DiaryMediaEntity(
                    id = it.getLong("id"),
                    entryId = it.getLong("entryId"),
                    type = it.getString("type"),
                    filePath = mediaMap[it.optString("filePath")] ?: "",
                    durationSec = it.optInt("durationSec"),
                    createdAt = it.optLong("createdAt")
                )
            )
        }
    }

    private suspend fun restoreTutor(data: JSONObject) {
        forEachJson(data, "tutorIncome") {
            db.tutorDao().insertIncome(
                TutorIncomeEntity(
                    id = it.getLong("id"),
                    date = it.getString("date"),
                    studentName = it.optString("studentName"),
                    subject = it.optString("subject"),
                    startMinute = it.optInt("startMinute"),
                    durationMin = it.optInt("durationMin"),
                    income = it.optDouble("income").toFloat(),
                    note = it.optString("note"),
                    createdAt = it.optLong("createdAt")
                )
            )
        }
        forEachJson(data, "tutorSchedule") {
            db.tutorDao().insertSchedule(
                TutorScheduleEntity(
                    id = it.getLong("id"),
                    weekday = it.getInt("weekday"),
                    startMinute = it.getInt("startMinute"),
                    endMinute = it.getInt("endMinute"),
                    studentName = it.optString("studentName"),
                    subject = it.optString("subject"),
                    note = it.optString("note"),
                    fee = it.optDouble("fee").toFloat()
                )
            )
        }
    }

    private inline fun forEachJson(data: JSONObject, key: String, block: (JSONObject) -> Unit) {
        val arr = data.optJSONArray(key) ?: return
        for (i in 0 until arr.length()) {
            block(arr.getJSONObject(i))
        }
    }

}

// ---------- JSON 序列化扩展 ----------

private fun WorkoutSessionEntity.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("startTime", startTime)
    put("endTime", endTime); put("note", note)
}

private fun WorkoutExerciseEntity.toJson() = JSONObject().apply {
    put("id", id); put("sessionId", sessionId); put("exerciseId", exerciseId)
    put("exerciseName", exerciseName); put("orderIndex", orderIndex); put("note", note)
}

private fun WorkoutSetEntity.toJson() = JSONObject().apply {
    put("id", id); put("exerciseId", exerciseId); put("setNumber", setNumber)
    put("weightKg", weightKg); put("reps", reps); put("durationSec", durationSec)
    put("restSec", restSec); put("rpe", rpe)
}

private fun ExerciseEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("muscleGroup", muscleGroup); put("isCustom", isCustom)
}

private fun MealRecordEntity.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("mealType", mealType)
    put("photoPath", photoPath); put("createdAt", createdAt)
}

private fun FoodEntryEntity.toJson() = JSONObject().apply {
    put("id", id); put("mealId", mealId); put("name", name); put("grams", grams)
    put("calories", calories); put("protein", protein); put("carbs", carbs); put("fat", fat)
}

private fun FoodEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("caloriesPer100g", caloriesPer100g)
    put("proteinPer100g", proteinPer100g); put("carbsPer100g", carbsPer100g)
    put("fatPer100g", fatPer100g); put("category", category)
}

private fun BodyMetricEntity.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("weightKg", weightKg)
    put("chestCm", chestCm); put("waistCm", waistCm); put("hipCm", hipCm); put("createdAt", createdAt)
}

private fun BodyPhotoEntity.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("angle", angle); put("filePath", filePath); put("createdAt", createdAt)
}

private fun DiaryEntryEntity.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("mood", mood); put("moodScore", moodScore)
    put("text", text); put("createdAt", createdAt); put("updatedAt", updatedAt)
}

private fun DiaryMediaEntity.toJson() = JSONObject().apply {
    put("id", id); put("entryId", entryId); put("type", type); put("filePath", filePath)
    put("durationSec", durationSec); put("createdAt", createdAt)
}

private fun TutorIncomeEntity.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("studentName", studentName); put("subject", subject)
    put("startMinute", startMinute); put("durationMin", durationMin); put("income", income)
    put("note", note); put("createdAt", createdAt)
}

private fun TutorScheduleEntity.toJson() = JSONObject().apply {
    put("id", id); put("weekday", weekday); put("startMinute", startMinute)
    put("endMinute", endMinute); put("studentName", studentName); put("subject", subject)
    put("note", note); put("fee", fee)
}
