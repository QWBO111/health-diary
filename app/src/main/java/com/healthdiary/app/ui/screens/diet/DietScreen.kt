package com.healthdiary.app.ui.screens.diet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.healthdiary.app.data.local.FoodEntity
import com.healthdiary.app.data.local.FoodEntryEntity
import com.healthdiary.app.data.local.MealRecordWithFood
import com.healthdiary.app.ui.components.DateSelector
import com.healthdiary.app.ui.components.SectionCard
import com.healthdiary.app.util.toDisplayString
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

private val MEAL_TYPES = listOf("早餐", "午餐", "晚餐", "加餐")
private val MEAL_STYLES = listOf(
    MealStyle("🌅", "早餐"),
    MealStyle("☀️", "午餐"),
    MealStyle("🌙", "晚餐"),
    MealStyle("🍎", "加餐")
)

private val PROTEIN_COLOR = Color(0xFFEF9A9A)
private val CARBS_COLOR = Color(0xFF90CAF9)
private val FAT_COLOR = Color(0xFFFFCC80)

private data class MealStyle(val icon: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(viewModel: DietViewModel = viewModel()) {
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    var addingFoodForMeal by remember { mutableStateOf<Long?>(null) }
    var photoTargetMealId by remember { mutableStateOf<Long?>(null) }
    var viewPhotoPath by remember { mutableStateOf<String?>(null) }
    var confirmDeleteMeal by remember { mutableStateOf<MealRecordWithFood?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DateSelector(viewModel.date) { viewModel.changeDate(it) }

            NutritionOverview(meals)

            MEAL_TYPES.forEachIndexed { index, mealType ->
                MealSection(
                    style = MEAL_STYLES[index],
                    meals = meals.filter { it.meal.mealType == mealType },
                    onAddMeal = { viewModel.addMeal(mealType) { } },
                    onAddFood = { mealId -> addingFoodForMeal = mealId },
                    onAddPhoto = { mealId ->
                        photoTargetMealId = mealId
                        mealPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDeleteMeal = { confirmDeleteMeal = it },
                    onDeleteFood = { viewModel.deleteFood(it) },
                    onViewPhoto = { viewPhotoPath = it }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    addingFoodForMeal?.let { mealId ->
        AddFoodDialog(
            onSearch = { query, onResult -> viewModel.searchFoods(query, onResult) },
            onConfirm = { name, grams, kcal, protein, carbs, fat ->
                viewModel.addFood(mealId, name, grams, kcal, protein, carbs, fat)
                addingFoodForMeal = null
            },
            onSaveToLibrary = { name, kcal, protein, carbs, fat ->
                viewModel.saveFoodToLibrary(name, kcal, protein, carbs, fat)
            },
            onDismiss = { addingFoodForMeal = null }
        )
    }

    confirmDeleteMeal?.let { meal ->
        AlertDialog(
            onDismissRequest = { confirmDeleteMeal = null },
            title = { Text("删除这一餐？") },
            text = { Text("该餐包含的 ${meal.foods.size} 种食物记录也会一并删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMeal(meal)
                        confirmDeleteMeal = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteMeal = null }) {
                    Text("取消")
                }
            }
        )
    }

    viewPhotoPath?.let { path ->
        PhotoViewerDialog(path = path, onDismiss = { viewPhotoPath = null })
    }
}

@Composable
private fun NutritionOverview(meals: List<MealRecordWithFood>) {
    val foods = meals.flatMap { it.foods }
    val kcal = foods.sumOf { it.calories.toDouble() }.roundToInt()
    val protein = foods.sumOf { it.protein.toDouble() }.roundToInt()
    val carbs = foods.sumOf { it.carbs.toDouble() }.roundToInt()
    val fat = foods.sumOf { it.fat.toDouble() }.roundToInt()
    val pKcal = protein * 4.0
    val cKcal = carbs * 4.0
    val fKcal = fat * 9.0
    val total = pKcal + cKcal + fKcal
    // avoid NaN when no macro nutrients are recorded (total == 0)
    val safeTotal = if (total > 0.0) total else 1.0
    val pWeight = (pKcal / safeTotal).toFloat()
    val cWeight = (cKcal / safeTotal).toFloat()
    val fWeight = (fKcal / safeTotal).toFloat()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日摄入",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = kcal.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "千卡",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Text(
                    text = "${foods.size} 项食物",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (foods.isEmpty()) {
                Text(
                    text = "今天还没有饮食记录，从第一餐开始吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                ) {
                    if (pWeight > 0f) {
                        Box(
                            Modifier
                                .weight(pWeight)
                                .fillMaxHeight()
                                .background(PROTEIN_COLOR)
                        )
                    }
                    if (cWeight > 0f) {
                        Box(
                            Modifier
                                .weight(cWeight)
                                .fillMaxHeight()
                                .background(CARBS_COLOR)
                        )
                    }
                    if (fWeight > 0f) {
                        Box(
                            Modifier
                                .weight(fWeight)
                                .fillMaxHeight()
                                .background(FAT_COLOR)
                        )
                    }
                }

                MacroRow(
                    label = "蛋白质",
                    grams = protein,
                    percent = (pKcal / safeTotal * 100).roundToInt(),
                    color = PROTEIN_COLOR
                )
                MacroRow(
                    label = "碳水",
                    grams = carbs,
                    percent = (cKcal / safeTotal * 100).roundToInt(),
                    color = CARBS_COLOR
                )
                MacroRow(
                    label = "脂肪",
                    grams = fat,
                    percent = (fKcal / safeTotal * 100).roundToInt(),
                    color = FAT_COLOR
                )
            }
        }
    }
}

@Composable
private fun MacroRow(label: String, grams: Int, percent: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${grams}g",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun MealSection(
    style: MealStyle,
    meals: List<MealRecordWithFood>,
    onAddMeal: () -> Unit,
    onAddFood: (Long) -> Unit,
    onAddPhoto: (Long) -> Unit,
    onDeleteMeal: (MealRecordWithFood) -> Unit,
    onDeleteFood: (FoodEntryEntity) -> Unit,
    onViewPhoto: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(style.icon, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val foods = meals.flatMap { it.foods }
                    val kcal = foods.sumOf { it.calories.toDouble() }.roundToInt()
                    Text(
                        text = if (meals.isEmpty()) {
                            "还没有记录"
                        } else {
                            "${foods.size} 种食物 · ${kcal} 千卡"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (meals.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🍽️", fontSize = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "这一餐还没有记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onAddMeal) {
                        Text("添加一餐")
                    }
                }
            } else {
                meals.forEach { meal ->
                    meal.meal.photoPath?.let { path ->
                        AsyncImage(
                            model = File(path),
                            contentDescription = "餐食照片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onViewPhoto(path) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    meal.foods.forEach { food ->
                        FoodRow(food = food, onDelete = { onDeleteFood(food) })
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onAddFood(meal.meal.id) }) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("添加食物")
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { onAddPhoto(meal.meal.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = "添加餐食照片",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteMeal(meal) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除整餐",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodRow(
    food: FoodEntryEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "蛋白 ${food.protein.toDisplayString()}g · 碳水 ${food.carbs.toDisplayString()}g · 脂肪 ${food.fat.toDisplayString()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${food.grams.toDisplayString()}g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${food.calories.roundToInt()} 千卡",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "删除食物",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFoodDialog(
    onSearch: (String, (List<FoodEntity>) -> Unit) -> Unit,
    onConfirm: (String, Float, Float, Float, Float, Float) -> Unit,
    onSaveToLibrary: (String, Float, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodEntity>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var saveToLibrary by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searched = false
            return@LaunchedEffect
        }
        delay(300)
        val q = query
        onSearch(q) { list ->
            if (q == query) {
                results = list
                searched = true
            }
        }
    }

    val gramsValue = grams.toFloatOrNull() ?: 0f
    val kcalValue = kcal.toFloatOrNull() ?: 0f
    val totalKcal = if (kcalValue > 0f && gramsValue > 0f) {
        (kcalValue * gramsValue / 100f).roundToInt()
    } else {
        0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加食物") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索食物库") },
                    placeholder = { Text("如：鸡胸肉、米饭") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (results.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(Modifier.padding(4.dp)) {
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
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = food.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (food.category.isNotBlank()) {
                                            Text(
                                                text = food.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${food.caloriesPer100g.toDisplayString()} 千卡/100g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else if (searched && query.isNotBlank()) {
                    Text(
                        text = "未找到匹配食物，可手动填写下方信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("食物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "份量",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("50", "100", "150", "200", "300").forEach { g ->
                            FilterChip(
                                selected = grams == g,
                                onClick = { grams = g },
                                label = { Text("${g}g") }
                            )
                        }
                    }
                }

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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveToLibrary,
                        onCheckedChange = { saveToLibrary = it }
                    )
                    Text(
                        text = "同时保存到食物库",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (totalKcal > 0) {
                    Text(
                        text = "预计摄入约 $totalKcal 千卡",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim().ifBlank { "未命名食物" },
                        gramsValue,
                        kcalValue,
                        protein.toFloatOrNull() ?: 0f,
                        carbs.toFloatOrNull() ?: 0f,
                        fat.toFloatOrNull() ?: 0f
                    )
                    if (saveToLibrary) {
                        onSaveToLibrary(
                            name.trim().ifBlank { "未命名食物" },
                            kcalValue,
                            protein.toFloatOrNull() ?: 0f,
                            carbs.toFloatOrNull() ?: 0f,
                            fat.toFloatOrNull() ?: 0f
                        )
                    }
                },
                enabled = name.isNotBlank() && gramsValue > 0f
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

@Composable
private fun PhotoViewerDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = "查看照片",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
