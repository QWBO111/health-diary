package com.healthdiary.app.ui.screens.diet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.healthdiary.app.data.local.FoodEntity
import com.healthdiary.app.data.local.FoodEntryEntity
import com.healthdiary.app.data.local.MealRecordWithFood
import com.healthdiary.app.ui.components.DateSelector
import com.healthdiary.app.ui.components.EmptyHint
import com.healthdiary.app.ui.components.SectionCard
import com.healthdiary.app.util.toDisplayString
import java.io.File
import kotlin.math.roundToInt

private val MEAL_TYPES = listOf("早餐", "午餐", "晚餐", "加餐")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(viewModel: DietViewModel = viewModel()) {
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    var addingFoodForMeal by remember { mutableStateOf<Long?>(null) }
    var photoTargetMealId by remember { mutableStateOf<Long?>(null) }

    val mealPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val mealId = photoTargetMealId
        if (uri != null && mealId != null) {
            viewModel.addMealPhoto(mealId, uri)
        }
        photoTargetMealId = null
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("饮食记录") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DateSelector(viewModel.date) { viewModel.changeDate(it) }

            MEAL_TYPES.forEach { mealType ->
                MealSection(
                    mealType = mealType,
                    meals = meals.filter { it.meal.mealType == mealType },
                    onAddMeal = { viewModel.addMeal(mealType) { } },
                    onAddFood = { mealId -> addingFoodForMeal = mealId },
                    onAddPhoto = { mealId ->
                        photoTargetMealId = mealId
                        mealPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDeleteMeal = { viewModel.deleteMeal(it) },
                    onDeleteFood = { viewModel.deleteFood(it) }
                )
            }

            SectionCard("今日合计") {
                val foods = meals.flatMap { it.foods }
                val kcal = foods.sumOf { it.calories.toDouble() }.roundToInt()
                val protein = foods.sumOf { it.protein.toDouble() }.roundToInt()
                val carbs = foods.sumOf { it.carbs.toDouble() }.roundToInt()
                val fat = foods.sumOf { it.fat.toDouble() }.roundToInt()
                Text("热量 ${kcal} 千卡 · 蛋白质 ${protein}g · 碳水 ${carbs}g · 脂肪 ${fat}g")
            }
        }
    }

    addingFoodForMeal?.let { mealId ->
        AddFoodDialog(
            onSearch = { query, onResult -> viewModel.searchFoods(query, onResult) },
            onConfirm = { name, grams, kcal, protein, carbs, fat ->
                viewModel.addFood(mealId, name, grams, kcal, protein, carbs, fat)
                addingFoodForMeal = null
            },
            onDismiss = { addingFoodForMeal = null }
        )
    }
}

@Composable
private fun MealSection(
    mealType: String,
    meals: List<MealRecordWithFood>,
    onAddMeal: () -> Unit,
    onAddFood: (Long) -> Unit,
    onAddPhoto: (Long) -> Unit,
    onDeleteMeal: (MealRecordWithFood) -> Unit,
    onDeleteFood: (FoodEntryEntity) -> Unit
) {
    SectionCard(mealType) {
        if (meals.isEmpty()) {
            EmptyHint("还没有记录")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAddMeal) {
                Text("添加一餐")
            }
        } else {
            meals.forEach { meal ->
                meal.foods.forEach { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(food.name, modifier = Modifier.weight(1f))
                        Text(
                            "${food.grams.toDisplayString()}克 · ${food.calories.roundToInt()} 千卡",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onDeleteFood(food) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除食物",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onAddFood(meal.meal.id) }) {
                        Text("+ 添加食物")
                    }
                    meal.meal.photoPath?.let { path ->
                        AsyncImage(
                            model = File(path),
                            contentDescription = "餐食照片",
                            modifier = Modifier.size(40.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(onClick = { onAddPhoto(meal.meal.id) }) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = "拍餐食照片")
                    }
                    IconButton(onClick = { onDeleteMeal(meal) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除整餐")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFoodDialog(
    onSearch: (String, (List<FoodEntity>) -> Unit) -> Unit,
    onConfirm: (String, Float, Float, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodEntity>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var searchTrigger by remember { mutableStateOf("") }

    fun runSearch() {
        searchTrigger = query
        onSearch(query) { list -> results = list }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加食物") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("从食物库搜索") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = { runSearch() }) {
                        Text("搜索")
                    }
                }
                if (results.isNotEmpty()) {
                    Card {
                        Column(Modifier.padding(8.dp)) {
                            results.take(5).forEach { food ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = food.name
                                            kcal = food.caloriesPer100g.toDisplayString()
                                            protein = food.proteinPer100g.toDisplayString()
                                            carbs = food.carbsPer100g.toDisplayString()
                                            fat = food.fatPer100g.toDisplayString()
                                            results = emptyList()
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(food.name, modifier = Modifier.weight(1f))
                                    Text(
                                        "${food.caloriesPer100g.toDisplayString()} 千卡/100克",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("食物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("食用克数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text("每100克热量（千卡）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("蛋白g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("碳水g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("脂肪g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val gramsValue = grams.toFloatOrNull() ?: 0f
                    val kcalValue = kcal.toFloatOrNull() ?: 0f
                    onConfirm(
                        name.trim().ifBlank { "未命名食物" },
                        gramsValue,
                        kcalValue,
                        protein.toFloatOrNull() ?: 0f,
                        carbs.toFloatOrNull() ?: 0f,
                        fat.toFloatOrNull() ?: 0f
                    )
                }
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
