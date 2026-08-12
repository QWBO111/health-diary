package com.healthdiary.app.ui.screens.diet

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.FoodEntity
import com.healthdiary.app.data.local.FoodEntryEntity
import com.healthdiary.app.data.local.MealRecordWithFood
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DietViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HealthDiaryApp).container

    var date: String by mutableStateOf(Dates.today())
        private set

    val meals: StateFlow<List<MealRecordWithFood>> =
        container.dietRepository.mealsByDate(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeDate(offset: Int) {
        date = runCatching {
            LocalDate.parse(date).plusDays(offset.toLong()).toString()
        }.getOrDefault(Dates.today())
    }

    fun addMeal(mealType: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = container.dietRepository.addMeal(date, mealType)
            onCreated(id)
        }
    }

    fun addFood(
        mealId: Long,
        name: String,
        grams: Float,
        kcalPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float
    ) {
        viewModelScope.launch {
            container.dietRepository.addFood(
                mealId, name, grams, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g
            )
        }
    }

    fun deleteFood(food: FoodEntryEntity) {
        viewModelScope.launch { container.dietRepository.deleteFood(food) }
    }

    fun deleteMeal(meal: MealRecordWithFood) {
        viewModelScope.launch { container.dietRepository.deleteMealWithFood(meal) }
    }

    fun addMealPhoto(mealId: Long, uri: Uri) {
        viewModelScope.launch { container.dietRepository.addMealPhoto(mealId, uri) }
    }

    fun searchFoods(query: String, onResult: (List<FoodEntity>) -> Unit) {
        viewModelScope.launch {
            onResult(container.dietRepository.searchFoods(query.trim()))
        }
    }

    fun saveFoodToLibrary(
        name: String,
        kcalPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float
    ) {
        viewModelScope.launch {
            container.dietRepository.addFoodToLibrary(
                FoodEntity(
                    name = name,
                    caloriesPer100g = kcalPer100g,
                    proteinPer100g = proteinPer100g,
                    carbsPer100g = carbsPer100g,
                    fatPer100g = fatPer100g
                )
            )
        }
    }
}
