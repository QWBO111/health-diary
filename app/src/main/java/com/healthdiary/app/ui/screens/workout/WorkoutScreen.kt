package com.healthdiary.app.ui.screens.workout

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthdiary.app.data.local.DailyWorkoutStat
import com.healthdiary.app.util.Dates
import com.healthdiary.app.util.volumeDisplay
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onStartWorkout: () -> Unit,
    onOpenDay: (String) -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("训练记录") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartWorkout) {
                Icon(Icons.Outlined.Add, contentDescription = "开始训练")
            }
        }
    ) { padding ->
        if (dailyStats.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "还没有训练记录\n点右下角开始第一次训练吧",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "weekly") { WeeklyOverviewCard(weeklyStats) }
                items(dailyStats, key = { it.date }) { stat ->
                    DailyStatCard(
                        stat = stat,
                        onClick = { onOpenDay(stat.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyOverviewCard(stats: List<DailyWorkoutStat>) {
    val sessionCount = stats.sumOf { it.sessionCount }
    val totalDurationMs = stats.sumOf { it.totalDurationMs }
    val totalVolumeKg = stats.sumOf { it.totalVolumeKg.toDouble() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "本周概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "总容量 ${volumeDisplay(totalVolumeKg)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                OverviewItem("训练", "${sessionCount} 次", Modifier.weight(1f))
                OverviewItem("时长", Dates.formatDuration(totalDurationMs), Modifier.weight(1f))
                OverviewItem("容量", volumeDisplay(totalVolumeKg), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            WeeklyVolumeChart(stats)
        }
    }
}

@Composable
private fun OverviewItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyVolumeChart(stats: List<DailyWorkoutStat>) {
    val maxVolume = stats.maxOfOrNull { it.totalVolumeKg }?.takeIf { it > 0f } ?: 1f
    val today = LocalDate.now().toString()
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = primary)

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val topPad = 18.dp.toPx()
            val spacing = 8.dp.toPx()
            val barArea = size.width - spacing * (stats.size - 1)
            val barWidth = barArea / stats.size
            val chartHeight = size.height - topPad - 4.dp.toPx()
            stats.forEachIndexed { index, stat ->
                val left = index * (barWidth + spacing)
                val ratio = (stat.totalVolumeKg / maxVolume).toDouble().coerceIn(0.0, 1.0)
                val barHeight = if (stat.totalVolumeKg > 0f) {
                    (chartHeight * ratio).toFloat().coerceAtLeast(8.dp.toPx())
                } else {
                    4.dp.toPx()
                }
                val isToday = stat.date == today
                val top = size.height - barHeight
                drawRoundRect(
                    color = if (isToday) primary else primaryContainer,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
                if (stat.totalVolumeKg > 0f) {
                    val label = if (stat.totalVolumeKg >= 1000f) {
                        String.format(Locale.US, "%.1ft", stat.totalVolumeKg / 1000f)
                    } else {
                        "${stat.totalVolumeKg.toInt()}kg"
                    }
                    val layout = textMeasurer.measure(AnnotatedString(label), style = labelStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            left + (barWidth - layout.size.width) / 2f,
                            (top - 15.dp.toPx()).coerceAtLeast(0f)
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            stats.forEach { stat ->
                val isToday = stat.date == today
                val label = Dates.formatWeekday(stat.date).removePrefix("星期")
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) primary else onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyStatCard(
    stat: DailyWorkoutStat,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateBadge(stat.date)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "训练 ${stat.sessionCount} 次",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "时长 ${Dates.formatDuration(stat.totalDurationMs)} · ${stat.exerciseCount} 个动作 · ${stat.totalSets} 组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    volumeDisplay(stat.totalVolumeKg.toDouble()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "总容量",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DateBadge(date: String) {
    val parts = date.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val weekday = Dates.formatWeekday(date).removePrefix("星期")

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$day",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "${month}月 · 周$weekday",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
