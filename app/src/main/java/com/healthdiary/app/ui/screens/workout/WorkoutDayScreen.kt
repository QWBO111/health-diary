package com.healthdiary.app.ui.screens.workout

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthdiary.app.data.local.WorkoutExerciseWithSets
import com.healthdiary.app.data.local.WorkoutSessionWithDetailsAndSets
import com.healthdiary.app.data.local.WorkoutSetEntity
import com.healthdiary.app.ui.components.EmptyHint
import com.healthdiary.app.ui.components.StatTile
import com.healthdiary.app.util.Dates
import com.healthdiary.app.util.toDisplayString
import com.healthdiary.app.util.volumeDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDayScreen(
    date: String,
    onBack: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    viewModel: WorkoutDayViewModel = viewModel()
) {
    LaunchedEffect(date) { viewModel.init(date) }
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            Dates.formatChinese(date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            Dates.formatWeekday(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyHint("这一天还没有训练记录")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") { DaySummaryTiles(sessions) }
                items(sessions, key = { it.session.id }) { session ->
                    DaySessionCard(
                        session = session,
                        onEdit = { onEditWorkout(session.session.id) },
                        onDelete = { pendingDelete = session.session.id }
                    )
                }
            }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除训练") },
            text = { Text("确定删除这次训练记录吗？动作和组数据会一并删除，无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(id)
                        pendingDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DaySummaryTiles(sessions: List<WorkoutSessionWithDetailsAndSets>) {
    val sessionCount = sessions.size
    val totalDurationMs = sessions.sumOf { it.session.endTime?.minus(it.session.startTime) ?: 0L }
    val exerciseCount = sessions.sumOf { it.exercises.size }
    val setCount = sessions.sumOf { session -> session.exercises.sumOf { it.sets.size } }
    val totalVolumeKg = sessions.sumOf { session ->
        session.exercises.sumOf { exercise ->
            exercise.sets.sumOf { set -> (set.weightKg * set.reps).toDouble() }
        }
    }
    val totalReps = sessions.sumOf { session ->
        session.exercises.sumOf { exercise -> exercise.sets.sumOf { it.reps } }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile("训练次数", "$sessionCount 次", Modifier.weight(1f))
            StatTile("总时长", Dates.formatDuration(totalDurationMs), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile("动作数", "$exerciseCount 个", Modifier.weight(1f))
            StatTile("总组数", "$setCount 组", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "总容量 ${volumeDisplay(totalVolumeKg)} · 总次数 $totalReps 次",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DaySessionCard(
    session: WorkoutSessionWithDetailsAndSets,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${Dates.formatTime(session.session.startTime)} - " +
                            (session.session.endTime?.let { Dates.formatTime(it) } ?: "进行中"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    session.session.endTime?.let {
                        Text(
                            "时长 ${Dates.formatDuration(it - session.session.startTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onEdit) { Text("编辑") }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (session.session.note.isNotBlank()) {
                Text(
                    session.session.note,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            session.exercises.forEachIndexed { index, exercise ->
                ExerciseDetailRow(index = index, exercise = exercise)
                if (index < session.exercises.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailRow(index: Int, exercise: WorkoutExerciseWithSets) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                exercise.exercise.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${exercise.sets.size} 组",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        exercise.sets.forEachIndexed { setIndex, set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 3.dp)
            ) {
                Text(
                    "第${setIndex + 1}组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(46.dp)
                )
                Text(
                    setDetailText(set),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (set.restSec > 0) {
                    Text(
                        "休${set.restSec}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun setDetailText(set: WorkoutSetEntity): String = when {
    set.reps > 0 && set.weightKg > 0f -> "${set.weightKg.toDisplayString()}kg × ${set.reps}"
    set.reps > 0 -> "${set.reps} 次"
    set.durationSec > 0 -> "${set.durationSec} 秒"
    else -> "—"
}
