package com.healthdiary.app.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthdiary.app.ui.components.EmptyHint
import com.healthdiary.app.ui.components.SectionCard
import com.healthdiary.app.ui.navigation.Destination
import com.healthdiary.app.util.Dates
import com.healthdiary.app.util.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigate: (String) -> Unit,
    viewModel: TodayViewModel = viewModel()
) {
    val workouts by viewModel.workoutsToday.collectAsStateWithLifecycle()
    val meals by viewModel.mealsToday.collectAsStateWithLifecycle()
    val weightToday by viewModel.weightToday.collectAsStateWithLifecycle()
    val diary by viewModel.diaryToday.collectAsStateWithLifecycle()
    val latestWeight by viewModel.latestWeight.collectAsStateWithLifecycle()
    val tutorIncome by viewModel.tutorIncomeToday.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日 · ${Dates.formatChinese(viewModel.today)}") },
                actions = {
                    IconButton(onClick = { onNavigate(Destination.Settings.route) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard("训练") {
                    if (workouts.isEmpty()) {
                        EmptyHint("今天还没有训练记录")
                    } else {
                        val session = workouts.first()
                        Text("已完成 ${workouts.size} 次训练")
                        session.endTime?.let {
                            Text("时长 ${Dates.formatDuration(it - session.startTime)}")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onNavigate(Destination.Workout.route) }) {
                        Text(if (workouts.isEmpty()) "去记录" else "查看详情")
                    }
                }
            }
            item {
                SectionCard("饮食") {
                    val foods = meals.flatMap { it.foods }
                    val kcal = foods.sumOf { it.calories.toDouble() }
                    if (meals.isEmpty()) {
                        EmptyHint("今天还没有饮食记录")
                    } else {
                        Text("已记录 ${meals.size} 餐、${foods.size} 项食物")
                        Text("合计 ${kcal.toInt()} 千卡")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onNavigate(Destination.Diet.route) }) {
                        Text(if (meals.isEmpty()) "去记录" else "查看详情")
                    }
                }
            }
            item {
                SectionCard("身体") {
                    val weight = weightToday?.weightKg ?: latestWeight?.weightKg
                    if (weight == null) {
                        EmptyHint("还没有体重记录")
                    } else {
                        Text("当前体重 ${weight.toDisplayString()} kg")
                        latestWeight?.let {
                            if (weightToday == null) {
                                Text("最近记录于 ${Dates.formatChinese(it.date)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onNavigate(Destination.Body.route) }) {
                        Text("去记录")
                    }
                }
            }
            item {
                SectionCard("日记") {
                    if (diary == null) {
                        EmptyHint("今天还没写日记")
                    } else {
                        Row {
                            Text("${diary!!.mood}  ", style = MaterialTheme.typography.titleLarge)
                            Text(
                                diary!!.text.ifBlank { "（心情 ${diary!!.moodScore} 分）" },
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onNavigate(Destination.Diary.route) }) {
                        Text(if (diary == null) "去写日记" else "查看日记")
                    }
                }
            }
            item {
                SectionCard("家教") {
                    if (tutorIncome.isEmpty()) {
                        EmptyHint("今天还没有家教收入记录")
                    } else {
                        val income = tutorIncome.sumOf { it.income.toDouble() }
                        val minutes = tutorIncome.sumOf { it.durationMin }
                        Text("今天 ${tutorIncome.size} 节课 · 收入 ¥${income.toInt()} · ${minutes} 分钟")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onNavigate(Destination.Tutor.route) }) {
                        Text(if (tutorIncome.isEmpty()) "去记录" else "查看详情")
                    }
                }
            }
        }
    }
}
