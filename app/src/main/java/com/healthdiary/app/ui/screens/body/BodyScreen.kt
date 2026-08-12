package com.healthdiary.app.ui.screens.body

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.healthdiary.app.data.local.BodyPhotoEntity
import com.healthdiary.app.ui.components.DateSelector
import com.healthdiary.app.ui.components.EmptyHint
import com.healthdiary.app.ui.components.SectionCard
import com.healthdiary.app.util.Dates
import com.healthdiary.app.util.toDisplayString
import java.io.File
import java.time.LocalDate
import java.util.Locale

private val ANGLES = listOf("正面", "侧面", "背面")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(viewModel: BodyViewModel = viewModel()) {
    val metric by viewModel.metric.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val allMetrics by viewModel.allMetrics.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allPhotos.collectAsStateWithLifecycle()
    val heightCm by viewModel.heightCm.collectAsStateWithLifecycle()

    var weightInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var hipInput by remember { mutableStateOf("") }
    var photoAngle by remember { mutableStateOf<String?>(null) }
    var compareAngle by remember { mutableStateOf<String?>(null) }
    var photoToDelete by remember { mutableStateOf<BodyPhotoEntity?>(null) }

    LaunchedEffect(metric) {
        weightInput = metric?.weightKg?.toDisplayString() ?: ""
        chestInput = metric?.chestCm?.toDisplayString() ?: ""
        waistInput = metric?.waistCm?.toDisplayString() ?: ""
        hipInput = metric?.hipCm?.toDisplayString() ?: ""
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val angle = photoAngle
        if (uri != null && angle != null) {
            viewModel.addPhoto(angle, uri)
        }
        photoAngle = null
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("身体记录") }) }
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

            BodyStatsCard(
                allMetrics = allMetrics,
                todayWeight = metric?.weightKg,
                heightCm = heightCm
            )

            SectionCard("体重与围度") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("体重 (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = { viewModel.saveWeight(weightInput) }) {
                        Text("保存")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = chestInput,
                        onValueChange = { chestInput = it },
                        label = { Text("胸围cm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = waistInput,
                        onValueChange = { waistInput = it },
                        label = { Text("腰围cm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hipInput,
                        onValueChange = { hipInput = it },
                        label = { Text("臀围cm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.saveMeasurements(chestInput, waistInput, hipInput)
                }) {
                    Text("保存围度")
                }
            }

            SectionCard("体重趋势") {
                val points = allMetrics
                    .filter { it.weightKg != null }
                    .map { it.date to it.weightKg!! }
                WeightChart(points)
            }

            SectionCard("姿态照片") {
                Text(
                    "建议固定机位，保持同一角度拍摄，对比效果更佳",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                ANGLES.forEach { angle ->
                    val anglePhotos = photos.filter { it.angle == angle }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            angle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (allPhotos.count { it.angle == angle } >= 2) {
                            TextButton(onClick = { compareAngle = angle }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.CompareArrows,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("对比")
                            }
                        }
                        IconButton(onClick = {
                            photoAngle = angle
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }) {
                            Icon(Icons.Outlined.Add, contentDescription = "添加${angle}照片")
                        }
                    }
                    if (anglePhotos.isEmpty()) {
                        EmptyHint("还没有${angle}照片", Modifier.padding(bottom = 8.dp))
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            anglePhotos.forEach { photo ->
                                Box {
                                    AsyncImage(
                                        model = File(photo.filePath),
                                        contentDescription = "${angle}照片",
                                        modifier = Modifier
                                            .size(92.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.55f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            Dates.formatChinese(photo.date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { photoToDelete = photo },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(30.dp)
                                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(15.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    compareAngle?.let { angle ->
        CompareDialog(
            angle = angle,
            allPhotos = allPhotos,
            onDismiss = { compareAngle = null }
        )
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("删除照片") },
            text = { Text("确定删除这张${photo.angle}照片吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePhoto(photo)
                        photoToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BodyStatsCard(
    allMetrics: List<com.healthdiary.app.data.local.BodyMetricEntity>,
    todayWeight: Float?,
    heightCm: Int
) {
    val weights = allMetrics.mapNotNull { it.weightKg }
    val current = todayWeight ?: weights.lastOrNull()
    val previous = weights.dropLast(1).lastOrNull()
    val bmi = if (current != null && heightCm > 0) {
        current / ((heightCm / 100f) * (heightCm / 100f))
    } else {
        null
    }
    val delta = if (current != null && previous != null) current - previous else null

    val todayStr = LocalDate.now().toString()
    val weekAgoStr = LocalDate.now().minusDays(7).toString()
    val weekAgoWeight = allMetrics
        .asSequence()
        .filter { it.weightKg != null && it.date in weekAgoStr..todayStr }
        .minByOrNull { it.date }
        ?.weightKg
    val weekDelta = if (current != null && weekAgoWeight != null) current - weekAgoWeight else null
    val firstWeight = weights.firstOrNull()
    val cumulativeDelta = if (current != null && firstWeight != null) current - firstWeight else null

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "当前体重",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        current?.toDisplayString() ?: "--",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        " kg",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    bmi?.let { "BMI ${String.format(Locale.US, "%.1f", it)}" } ?: "身高未设置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "共 ${weights.size} 条记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                DeltaCell("较上次", delta)
                Spacer(Modifier.height(8.dp))
                DeltaCell("较7天前", weekDelta)
                Spacer(Modifier.height(8.dp))
                DeltaCell("累计变化", cumulativeDelta)
            }
        }
    }
}

@Composable
private fun DeltaCell(label: String, delta: Float?) {
    val valueColor = when {
        delta == null -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        delta < -0.05f -> Color(0xFF2E7D32)
        delta > 0.05f -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            if (delta == null) "--" else if (delta > 0f) "+${delta.toDisplayString()}" else delta.toDisplayString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun WeightChart(points: List<Pair<String, Float>>) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        if (points.size < 2) {
            EmptyHint("至少记录两天体重后显示趋势", Modifier.align(Alignment.Center))
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val min = points.minOf { it.second }
                val max = points.maxOf { it.second }
                val span = (max - min).coerceAtLeast(1f)
                val leftPad = 36.dp.toPx()
                val rightPad = 12.dp.toPx()
                val topPad = 22.dp.toPx()
                val bottomPad = 26.dp.toPx()
                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad
                val step = chartW / (points.size - 1)

                repeat(4) { i ->
                    val y = topPad + chartH * i / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + chartW, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val line = Path()
                points.forEachIndexed { index, (_, weight) ->
                    val x = leftPad + step * index
                    val y = topPad + chartH * (1f - (weight - min) / span)
                    if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }

                val area = Path().apply {
                    val firstX = leftPad
                    val lastX = leftPad + step * (points.size - 1)
                    moveTo(firstX, topPad + chartH)
                    lineTo(firstX, topPad + chartH * (1f - (points.first().second - min) / span))
                    points.forEachIndexed { index, (_, weight) ->
                        val x = leftPad + step * index
                        val y = topPad + chartH * (1f - (weight - min) / span)
                        lineTo(x, y)
                    }
                    lineTo(lastX, topPad + chartH)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.28f),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = topPad,
                        endY = topPad + chartH
                    )
                )

                drawPath(
                    path = line,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { index, (_, weight) ->
                    val x = leftPad + step * index
                    val y = topPad + chartH * (1f - (weight - min) / span)
                    val isLast = index == points.lastIndex
                    drawCircle(lineColor, radius = if (isLast) 5.dp.toPx() else 3.5.dp.toPx(), center = Offset(x, y))
                    if (isLast) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 9.dp.toPx(),
                            center = Offset(x, y),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                val lastWeight = points.last().second
                val lastX = leftPad + chartW
                val lastY = topPad + chartH * (1f - (lastWeight - min) / span)
                val valueStyle = TextStyle(
                    fontSize = 11.sp,
                    color = onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                val valueLayout = textMeasurer.measure(
                    AnnotatedString("${lastWeight.toDisplayString()}kg"),
                    style = valueStyle
                )
                val chipW = valueLayout.size.width + 14.dp.toPx()
                val chipH = valueLayout.size.height + 8.dp.toPx()
                val chipX = (lastX - chipW / 2f).coerceIn(leftPad, size.width - rightPad - chipW)
                val chipY = (lastY - chipH - 8.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(
                    color = primaryContainer,
                    topLeft = Offset(chipX, chipY),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                drawText(
                    textLayoutResult = valueLayout,
                    topLeft = Offset(chipX + 7.dp.toPx(), chipY + 4.dp.toPx())
                )

                val maxText = "${max.toDisplayString()}kg"
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(maxText),
                    topLeft = Offset(0f, topPad - 12.dp.toPx()),
                    style = labelStyle
                )
                val minText = "${min.toDisplayString()}kg"
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(minText),
                    topLeft = Offset(0f, topPad + chartH - 8.dp.toPx()),
                    style = labelStyle
                )

                fun drawDate(date: String, x: Float) {
                    val short = runCatching {
                        val p = date.split("-")
                        "${p[1].toInt()}月${p[2].toInt()}日"
                    }.getOrDefault(date)
                    val width = textMeasurer.measure(AnnotatedString(short), style = labelStyle).size.width
                    drawText(
                        textMeasurer = textMeasurer,
                        text = AnnotatedString(short),
                        topLeft = Offset((x - width / 2f).coerceIn(leftPad, size.width - rightPad - width), size.height - 20.dp.toPx()),
                        style = labelStyle
                    )
                }
                if (points.size <= 7) {
                    points.forEachIndexed { index, (date, _) ->
                        drawDate(date, leftPad + step * index)
                    }
                } else {
                    drawDate(points.first().first, leftPad)
                    drawDate(points.last().first, leftPad + chartW)
                }
            }
        }
    }
}

@Composable
private fun CompareDialog(
    angle: String,
    allPhotos: List<BodyPhotoEntity>,
    onDismiss: () -> Unit
) {
    val photosForAngle = allPhotos
        .filter { it.angle == angle }
        .sortedBy { it.date }
    val first = photosForAngle.firstOrNull()
    val last = photosForAngle.lastOrNull()
    var progress by remember { mutableFloatStateOf(0.5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$angle 前后对比") },
        text = {
            if (first == null || last == null || first.filePath == last.filePath) {
                EmptyHint("需要至少两张不同日期的照片")
            } else {
                Column {
                    Text(
                        "${Dates.formatChinese(first.date)}  →  ${Dates.formatChinese(last.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = File(first.filePath),
                            contentDescription = "早期照片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = File(last.filePath),
                            contentDescription = "近期照片",
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    clipRect(right = size.width * progress) {
                                        this@drawWithContent.drawContent()
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "← 早期    近期 →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
