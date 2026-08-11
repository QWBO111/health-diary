package com.healthdiary.app.data.repository

import android.net.Uri
import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.local.FoodEntity
import com.healthdiary.app.data.local.FoodEntryEntity
import com.healthdiary.app.data.local.MealRecordEntity
import com.healthdiary.app.data.local.MealRecordWithFood
import com.healthdiary.app.data.media.MediaStore
import kotlinx.coroutines.flow.Flow

class DietRepository(
    private val db: AppDatabase,
    private val mediaStore: MediaStore
) {
    fun mealsByDate(date: String): Flow<List<MealRecordWithFood>> =
        db.dietDao().observeMeals(date)

    suspend fun mealsForDate(date: String): List<MealRecordWithFood> =
        db.dietDao().getMeals(date)

    suspend fun addMeal(date: String, mealType: String, photoPath: String? = null): Long =
        db.dietDao().insertMeal(
            MealRecordEntity(date = date, mealType = mealType, photoPath = photoPath)
        )

    suspend fun updateMealPhoto(mealId: Long, photoPath: String?) {
        val meal = db.dietDao().getMeal(mealId) ?: return
        db.dietDao().updateMeal(meal.copy(photoPath = photoPath))
    }

    suspend fun addMealPhoto(mealId: Long, uri: Uri): String {
        val path = mediaStore.copyPhotoFromUri(uri, "meal")
        updateMealPhoto(mealId, path)
        return path
    }

    suspend fun addFood(
        mealId: Long,
        name: String,
        grams: Float,
        kcalPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float
    ) {
        val factor = grams / 100f
        db.dietDao().insertFood(
            FoodEntryEntity(
                mealId = mealId,
                name = name,
                grams = grams,
                calories = kcalPer100g * factor,
                protein = proteinPer100g * factor,
                carbs = carbsPer100g * factor,
                fat = fatPer100g * factor
            )
        )
    }

    suspend fun deleteFood(food: FoodEntryEntity) {
        db.dietDao().deleteFood(food.id)
    }

    suspend fun deleteMealWithFood(meal: MealRecordWithFood) {
        meal.foods.forEach { db.dietDao().deleteFood(it.id) }
        mediaStore.delete(meal.meal.photoPath)
        db.dietDao().deleteMeal(meal.meal.id)
    }

    // ---------- 食物库 ----------

    suspend fun searchFoods(query: String): List<FoodEntity> =
        db.foodLibraryDao().search(query)

    suspend fun addFoodToLibrary(food: FoodEntity) {
        db.foodLibraryDao().insert(food)
    }
}
