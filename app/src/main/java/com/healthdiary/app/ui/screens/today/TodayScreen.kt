package com.healthdiary.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthdiary.app.ui.navigation.Destination
import com.healthdiary.app.util.Dates
import com.healthdiary.app.util.toDisplayString
import kotlin.math.roundToInt

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

    val kcal = meals.flatMap { it.foods }.sumOf { it.calories.toDouble() }.roundToInt()
    val weight = weightToday?.weightKg ?: latestWeight?.weightKg
    val income = tutorIncome.sumOf { it.income.toDouble() }.roundToInt()

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
                TodayHero(
                    workoutCount = workouts.size,
                    kcal = kcal,
                    weight = weight,
                    diaryDone = diary != null,
                    income = income,
                    lessonCount = tutorIncome.size
                )
            }
            item {
                ModuleCard(
                    icon = Icons.Outlined.FitnessCenter,
                    title = "训练",
                    summary = if (workouts.isEmpty()) {
                        "今天还没有训练记录"
                    } else {
                        "已完成 ${workouts.size} 次训练"
                    },
                    done = workouts.isNotEmpty(),
                    actionLabel = if (workouts.isEmpty()) "去记录" else "查看详情",
                    onClick = { onNavigate(Destination.Workout.route) }
                )
            }
            item {
                ModuleCard(
                    icon = Icons.Outlined.Restaurant,
                    title = "饮食",
                    summary = if (meals.isEmpty()) {
                        "今天还没有饮食记录"
                    } else {
                        "已记录 ${meals.size} 餐 · ${kcal} 千卡"
                    },
                    done = meals.isNotEmpty(),
                    actionLabel = if (meals.isEmpty()) "去记录" else "查看详情",
                    onClick = { onNavigate(Destination.Diet.route) }
                )
            }
            item {
                ModuleCard(
                    icon = Icons.Outlined.MonitorWeight,
                    title = "身体",
                    summary = if (weight == null) {
                        "还没有体重记录"
                    } else {
                        "当前体重 ${weight.toDisplayString()} kg"
                    },
                    done = weight != null,
                    actionLabel = "去记录",
                    onClick = { onNavigate(Destination.Body.route) }
                )
            }
            item {
                ModuleCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = "日记",
                    summary = if (diary == null) {
                        "今天还没写日记"
                    } else {
                        diary!!.text.ifBlank { "心情 ${diary!!.moodScore} 分" }
                    },
                    done = diary != null,
                    actionLabel = if (diary == null) "去写日记" else "查看日记",
                    onClick = { onNavigate(Destination.Diary.route) }
                )
            }
            item {
                ModuleCard(
                    icon = Icons.Outlined.School,
                    title = "家教",
                    summary = if (tutorIncome.isEmpty()) {
                        "今天还没有家教收入记录"
                    } else {
                        "今天 ${tutorIncome.size} 节课 · ¥$income"
                    },
                    done = tutorIncome.isNotEmpty(),
                    actionLabel = if (tutorIncome.isEmpty()) "去记录" else "查看详情",
                    onClick = { onNavigate(Destination.Tutor.route) }
                )
            }
        }
    }
}

@Composable
private fun TodayHero(
    workoutCount: Int,
    kcal: Int,
    weight: Float?,
    diaryDone: Boolean,
    income: Int,
    lessonCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "今日小结",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat("训练", "$workoutCount 次", Modifier.weight(1f))
                HeroDivider()
                HeroStat("饮食", "$kcal 千卡", Modifier.weight(1f))
                HeroDivider()
                HeroStat("体重", weight?.toDisplayString() ?: "--", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat("日记", if (diaryDone) "已写" else "未写", Modifier.weight(1f))
                HeroDivider()
                HeroStat("家教", "¥$income", Modifier.weight(1f))
                HeroDivider()
                HeroStat("课时", "$lessonCount 节", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))
    )
}

@Composable
private fun ModuleCard(
    icon: ImageVector,
    title: String,
    summary: String,
    done: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (done) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusPill(done)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StatusPill(done: Boolean) {
    val background = if (done) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val content = if (done) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = if (done) "已记录" else "待记录",
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = content
    )
}
