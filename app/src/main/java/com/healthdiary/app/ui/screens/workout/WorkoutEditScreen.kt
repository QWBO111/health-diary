package com.healthdiary.app.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthdiary.app.data.local.ExerciseEntity
import com.healthdiary.app.data.repository.ExerciseDraft
import com.healthdiary.app.util.Dates
import com.healthdiary.app.util.toDisplayString
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditScreen(
    sessionId: Long,
    onDone: () -> Unit,
    viewModel: WorkoutEditViewModel = viewModel()
) {
    LaunchedEffect(sessionId) { viewModel.init(sessionId) }

    val library by viewModel.library.collectAsStateWithLifecycle()
    val drafts = viewModel.drafts
    var showPicker by remember { mutableStateOf(sessionId < 0) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedMillis += 1000
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sessionId >= 0) "编辑训练" else "训练记录") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        Dates.formatDuration(elapsedMillis),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save(onDone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("完成并保存")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showPicker || drafts.isEmpty()) {
                ExercisePicker(
                    library = library,
                    selectedIds = drafts.mapNotNull { it.exerciseId }.toSet(),
                    onToggle = { exercise ->
                        if (exercise.id in drafts.mapNotNull { it.exerciseId }) {
                            viewModel.removeExerciseById(exercise.id)
                        } else {
                            viewModel.addExercise(exercise)
                        }
                    },
                    onAddCustom = { viewModel.addCustomExercise(it) },
                    onDone = { showPicker = false }
                )
            } else {
                drafts.forEachIndexed { index, draft ->
                    ExerciseDraftCard(
                        index = index,
                        draft = draft,
                        onAddSet = { viewModel.addSet(index) },
                        onRemoveSet = { setIndex -> viewModel.removeSet(index, setIndex) },
                        onUpdateSet = { setIndex, field, value ->
                            viewModel.updateSet(index, setIndex, field, value)
                        },
                        onRemoveExercise = { viewModel.removeExercise(index) }
                    )
                }
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加动作")
                }
                OutlinedTextField(
                    value = viewModel.note,
                    onValueChange = { viewModel.note = it },
                    label = { Text("训练备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ExercisePicker(
    library: List<ExerciseEntity>,
    selectedIds: Set<Long>,
    onToggle: (ExerciseEntity) -> Unit,
    onAddCustom: (String) -> Unit,
    onDone: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) library
    else library.filter { it.name.contains(query.trim(), ignoreCase = true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("选择动作", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索动作") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(filtered, key = { it.id }) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(exercise) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = exercise.id in selectedIds,
                            onCheckedChange = { onToggle(exercise) }
                        )
                        Column {
                            Text(exercise.name)
                            if (exercise.muscleGroup.isNotBlank()) {
                                Text(
                                    exercise.muscleGroup,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("自定义动作名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (customName.isNotBlank()) {
                            onAddCustom(customName)
                            customName = ""
                        }
                    }
                ) {
                    Text("添加")
                }
            }
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                Text(if (selectedIds.isEmpty()) "直接下一步" else "下一步，记录组数")
            }
        }
    }
}

@Composable
private fun ExerciseDraftCard(
    index: Int,
    draft: ExerciseDraft,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, SetField, String) -> Unit,
    onRemoveExercise: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    draft.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemoveExercise) {
                    Icon(Icons.Outlined.Delete, contentDescription = "移除动作")
                }
            }
            draft.sets.forEachIndexed { setIndex, set ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${setIndex + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.width(20.dp)
                    )
                    SetNumberField(
                        value = set.weightKg.takeIf { it > 0f }?.toDisplayString() ?: "",
                        onChange = { onUpdateSet(setIndex, SetField.WEIGHT, it) },
                        label = "重量kg",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    SetNumberField(
                        value = set.reps.takeIf { it > 0 }?.toString() ?: "",
                        onChange = { onUpdateSet(setIndex, SetField.REPS, it) },
                        label = "次数",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    SetNumberField(
                        value = set.durationSec.takeIf { it > 0 }?.toString() ?: "",
                        onChange = { onUpdateSet(setIndex, SetField.DURATION, it) },
                        label = "时长s",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveSet(setIndex) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "删除该组")
                    }
                }
            }
            FilledTonalButton(onClick = onAddSet) {
                Text("+ 添加一组")
            }
        }
    }
}

@Composable
private fun SetNumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
