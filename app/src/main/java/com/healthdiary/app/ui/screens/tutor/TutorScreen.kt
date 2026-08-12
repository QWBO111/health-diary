package com.healthdiary.app.ui.screens.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthdiary.app.data.local.TutorIncomeEntity
import com.healthdiary.app.data.local.TutorScheduleEntity
import com.healthdiary.app.ui.components.DateSelector
import com.healthdiary.app.ui.components.EmptyHint
import com.healthdiary.app.ui.components.SectionCard
import com.healthdiary.app.ui.components.StatTile
import java.time.LocalDate
import kotlin.math.roundToInt

private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private fun formatMinute(minute: Int): String =
    String.format("%02d:%02d", minute / 60, minute % 60)

private fun parseMinute(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

private fun formatDurationMin(min: Int): String = when {
    min <= 0 -> "0分钟"
    min < 60 -> "${min}分钟"
    min % 60 == 0 -> "${min / 60}小时"
    else -> "${min / 60}小时${min % 60}分"
}

private fun money(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format("%.1f", value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(viewModel: TutorViewModel = viewModel()) {
    val incomeRecords by viewModel.incomeByDate.collectAsStateWithLifecycle()
    val allIncome by viewModel.allIncome.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()

    var tabIndex by remember { mutableIntStateOf(0) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<TutorIncomeEntity?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<TutorScheduleEntity?>(null) }
    var deleteIncomeId by remember { mutableStateOf<Long?>(null) }
    var deleteScheduleId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("家教记录") })
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("课表") },
                        icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("收入") },
                        icon = { Icon(Icons.Outlined.Payments, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tabIndex == 0) {
                        editingSchedule = null
                        showScheduleDialog = true
                    } else {
                        editingIncome = null
                        showIncomeDialog = true
                    }
                }
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "新增")
            }
        }
    ) { padding ->
        when (tabIndex) {
            0 -> ScheduleTab(
                modifier = Modifier.padding(padding).fillMaxSize(),
                schedule = schedule,
                onEdit = { item ->
                    editingSchedule = item
                    showScheduleDialog = true
                },
                onDelete = { deleteScheduleId = it.id }
            )
            else -> IncomeTab(
                modifier = Modifier.padding(padding).fillMaxSize(),
                viewModel = viewModel,
                records = incomeRecords,
                allIncome = allIncome,
                onAdd = {
                    editingIncome = null
                    showIncomeDialog = true
                },
                onEdit = { record ->
                    editingIncome = record
                    showIncomeDialog = true
                },
                onDelete = { deleteIncomeId = it.id }
            )
        }
    }

    if (showIncomeDialog) {
        IncomeDialog(
            record = editingIncome,
            onConfirm = { student, subject, startMin, durationMin, income, note ->
                val record = editingIncome
                if (record == null) {
                    viewModel.addIncome(student, subject, startMin, durationMin, income, note)
                } else {
                    viewModel.updateIncome(record.id, student, subject, startMin, durationMin, income, note)
                }
                showIncomeDialog = false
            },
            onDismiss = { showIncomeDialog = false }
        )
    }

    if (showScheduleDialog) {
        ScheduleDialog(
            item = editingSchedule,
            defaultWeekday = LocalDate.now().dayOfWeek.value,
            onConfirm = { weekday, startMin, endMin, student, subject, note, fee, onResult ->
                val item = editingSchedule
                viewModel.checkConflict(weekday, startMin, endMin, item?.id ?: -1L) { conflict ->
                    if (!conflict) {
                        if (item == null) {
                            viewModel.addSchedule(weekday, startMin, endMin, student, subject, note, fee)
                        } else {
                            viewModel.updateSchedule(item.id, weekday, startMin, endMin, student, subject, note, fee)
                        }
                        showScheduleDialog = false
                    }
                    onResult(!conflict)
                }
            },
            onDelete = editingSchedule?.let { item ->
                {
                    deleteScheduleId = item.id
                    showScheduleDialog = false
                }
            },
            onDismiss = { showScheduleDialog = false }
        )
    }

    deleteIncomeId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteIncomeId = null },
            title = { Text("删除这笔收入记录？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIncome(id)
                        deleteIncomeId = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteIncomeId = null }) { Text("取消") }
            }
        )
    }

    deleteScheduleId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteScheduleId = null },
            title = { Text("删除这节排课？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(id)
                        deleteScheduleId = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteScheduleId = null }) { Text("取消") }
            }
        )
    }
}

// ---------------- 课表 ----------------

private val GRID_MIN_HOUR = 6
private val GRID_MAX_HOUR = 23
private val HOUR_HEIGHT = 56.dp
private val DAY_COLUMN_WIDTH = 108.dp

@Composable
private fun ScheduleTab(
    modifier: Modifier,
    schedule: List<TutorScheduleEntity>,
    onEdit: (TutorScheduleEntity) -> Unit,
    onDelete: (TutorScheduleEntity) -> Unit
) {
    val todayWeekday = LocalDate.now().dayOfWeek.value
    val totalMinutes = schedule.sumOf { (it.endMinute - it.startMinute).coerceAtLeast(0) }
    val totalFee = schedule.sumOf { it.fee.toDouble() }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard("每周安排") {
            Row(Modifier.fillMaxWidth()) {
                StatTile("每周课程", "${schedule.size} 节", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                StatTile("每周时长", formatDurationMin(totalMinutes), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                StatTile("每周课费", "¥${money(totalFee.toFloat())}", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                StatTile("今天", "${schedule.count { it.weekday == todayWeekday }} 节", Modifier.weight(1f))
            }
        }

        if (schedule.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🗓️", fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    EmptyHint("还没有排课，点右下角添加吧")
                }
            }
        } else {
            WeeklyGrid(
                schedule = schedule,
                todayWeekday = todayWeekday,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun WeeklyGrid(
    schedule: List<TutorScheduleEntity>,
    todayWeekday: Int,
    onEdit: (TutorScheduleEntity) -> Unit,
    onDelete: (TutorScheduleEntity) -> Unit
) {
    val minStart = schedule.minOf { it.startMinute }
    val maxEnd = schedule.maxOf { it.endMinute }
    val startHour = (minStart / 60).coerceIn(GRID_MIN_HOUR, GRID_MAX_HOUR - 1)
    val endHour = ((maxEnd + 59) / 60).coerceIn(startHour + 1, GRID_MAX_HOUR)
    val totalHours = endHour - startHour
    val totalHeight = HOUR_HEIGHT * totalHours

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(Modifier.width(44.dp)) {
            Spacer(Modifier.height(24.dp))
            repeat(totalHours) { h ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HOUR_HEIGHT),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        "${startHour + h}:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                    )
                }
            }
        }
        WEEKDAYS.forEachIndexed { index, label ->
            val weekday = index + 1
            val items = schedule
                .filter { it.weekday == weekday }
                .sortedBy { it.startMinute }
            DayColumn(
                label = label,
                isToday = weekday == todayWeekday,
                items = items,
                startHour = startHour,
                totalHours = totalHours,
                totalHeight = totalHeight,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun DayColumn(
    label: String,
    isToday: Boolean,
    items: List<TutorScheduleEntity>,
    startHour: Int,
    totalHours: Int,
    totalHeight: Dp,
    onEdit: (TutorScheduleEntity) -> Unit,
    onDelete: (TutorScheduleEntity) -> Unit
) {
    Column(Modifier.width(DAY_COLUMN_WIDTH)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .background(
                    if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                    else Color.Transparent
                )
        ) {
            repeat(totalHours + 1) { h ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = HOUR_HEIGHT * h)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                )
            }
            items.forEach { item ->
                val top = HOUR_HEIGHT * ((item.startMinute - startHour * 60).toFloat() / 60f)
                val duration = (item.endMinute - item.startMinute).coerceAtLeast(30)
                val blockHeight = HOUR_HEIGHT * (duration / 60f)
                val conflict = items.any {
                    it.id != item.id &&
                        item.startMinute < it.endMinute &&
                        item.endMinute > it.startMinute
                }
                ScheduleBlock(
                    item = item,
                    conflict = conflict,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .offset(y = top)
                        .height(blockHeight - 2.dp),
                    onClick = { onEdit(item) }
                )
            }
        }
    }
}

@Composable
private fun ScheduleBlock(
    item: TutorScheduleEntity,
    conflict: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val background = if (conflict) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (conflict) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Column {
            Text(
                "${formatMinute(item.startMinute)}-${formatMinute(item.endMinute)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
            Text(
                item.studentName + if (item.subject.isNotBlank()) "·${item.subject}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
            if (item.fee > 0f) {
                Text(
                    "¥${money(item.fee)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    item: TutorScheduleEntity,
    conflicts: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    formatMinute(item.startMinute),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    formatMinute(item.endMinute),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${item.studentName}${if (item.subject.isNotBlank()) " · ${item.subject}" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (conflicts) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "与当天其他课程时间重叠",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------- 收入 ----------------

@Composable
private fun IncomeTab(
    modifier: Modifier,
    viewModel: TutorViewModel,
    records: List<TutorIncomeEntity>,
    allIncome: List<TutorIncomeEntity>,
    onAdd: () -> Unit,
    onEdit: (TutorIncomeEntity) -> Unit,
    onDelete: (TutorIncomeEntity) -> Unit
) {
    val dayIncome = records.sumOf { it.income.toDouble() }
    val dayMinutes = records.sumOf { it.durationMin }
    val monthPrefix = viewModel.date.take(7)
    val monthIncome = allIncome.filter { it.date.startsWith(monthPrefix) }.sumOf { it.income.toDouble() }
    val monthMinutes = allIncome.filter { it.date.startsWith(monthPrefix) }.sumOf { it.durationMin }
    val monthCount = allIncome.count { it.date.startsWith(monthPrefix) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DateSelector(viewModel.date) { viewModel.changeDate(it) }

        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "当日收入",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "¥${money(dayIncome.toFloat())}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${records.size} 节课 · ${formatDurationMin(dayMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        SectionCard("本月汇总") {
            Row(Modifier.fillMaxWidth()) {
                StatTile("本月收入", "¥${money(monthIncome.toFloat())}", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                StatTile("本月课时", "$monthCount 节", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                StatTile("本月时长", formatDurationMin(monthMinutes), Modifier.weight(1f))
            }
        }

        if (records.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💰", fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    EmptyHint("这一天还没有收入记录，点右下角记一笔")
                }
            }
        } else {
            records.forEach { record ->
                IncomeCard(
                    record = record,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun IncomeCard(
    record: TutorIncomeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${record.studentName}${if (record.subject.isNotBlank()) " · ${record.subject}" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatMinute(record.startMinute)} - ${formatMinute(record.startMinute + record.durationMin)} · ${formatDurationMin(record.durationMin)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.note.isNotBlank()) {
                    Text(
                        record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "¥${money(record.income)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------- 弹窗 ----------------

@Composable
private fun IncomeDialog(
    record: TutorIncomeEntity?,
    onConfirm: (String, String, Int, Int, Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var student by remember { mutableStateOf(record?.studentName ?: "") }
    var subject by remember { mutableStateOf(record?.subject ?: "") }
    var startText by remember { mutableStateOf(formatMinute(record?.startMinute ?: (18 * 60))) }
    var durationText by remember { mutableStateOf((record?.durationMin ?: 60).toString()) }
    var incomeText by remember { mutableStateOf(record?.income?.let { money(it) } ?: "") }
    var note by remember { mutableStateOf(record?.note ?: "") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "记一笔收入" else "编辑收入记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = student,
                    onValueChange = { student = it },
                    label = { Text("学生姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("科目（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = { Text("开始时间") },
                        placeholder = { Text("18:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("时长(分钟)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = incomeText,
                        onValueChange = { incomeText = it },
                        label = { Text("收入(元)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("60", "90", "120").forEach { preset ->
                        FilterChip(
                            selected = durationText == preset,
                            onClick = { durationText = preset },
                            label = { Text("${preset}分钟") }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = parseMinute(startText)
                    val duration = durationText.toIntOrNull()
                    val income = incomeText.toFloatOrNull()
                    when {
                        student.isBlank() -> error = "请填写学生姓名"
                        start == null -> error = "开始时间格式应为 HH:mm"
                        duration == null || duration <= 0 -> error = "请填写正确的时长"
                        income == null || income <= 0f -> error = "请填写正确的收入"
                        else -> onConfirm(student.trim(), subject.trim(), start, duration, income, note.trim())
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ScheduleDialog(
    item: TutorScheduleEntity?,
    defaultWeekday: Int,
    onConfirm: (Int, Int, Int, String, String, String, Float, (Boolean) -> Unit) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var weekday by remember { mutableIntStateOf(item?.weekday ?: defaultWeekday) }
    var student by remember { mutableStateOf(item?.studentName ?: "") }
    var subject by remember { mutableStateOf(item?.subject ?: "") }
    var startText by remember { mutableStateOf(formatMinute(item?.startMinute ?: (18 * 60))) }
    var endText by remember { mutableStateOf(formatMinute(item?.endMinute ?: (20 * 60))) }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var feeText by remember {
        mutableStateOf(item?.fee?.takeIf { it > 0f }?.let { money(it) } ?: "")
    }
    var error by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var conflictError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "新增排课" else "编辑排课") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEEKDAYS.forEachIndexed { index, label ->
                        FilterChip(
                            selected = weekday == index + 1,
                            onClick = { weekday = index + 1 },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = student,
                    onValueChange = { student = it },
                    label = { Text("学生姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("科目（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = { Text("开始时间") },
                        placeholder = { Text("18:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        label = { Text("结束时间") },
                        placeholder = { Text("20:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("课费（元，选填）") },
                    placeholder = { Text("200") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (conflictError.isNotBlank()) {
                    Text(conflictError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !checking,
                onClick = {
                    val start = parseMinute(startText)
                    val end = parseMinute(endText)
                    when {
                        student.isBlank() -> error = "请填写学生姓名"
                        start == null -> error = "开始时间格式应为 HH:mm"
                        end == null -> error = "结束时间格式应为 HH:mm"
                        end <= start -> error = "结束时间必须晚于开始时间"
                        else -> {
                            checking = true
                            error = ""
                            conflictError = ""
                            val fee = feeText.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                            onConfirm(
                                weekday,
                                start,
                                end,
                                student.trim(),
                                subject.trim(),
                                note.trim(),
                                fee
                            ) { success ->
                                checking = false
                                if (!success) {
                                    conflictError = "该时间段已有排课，请调整时间"
                                }
                            }
                        }
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
