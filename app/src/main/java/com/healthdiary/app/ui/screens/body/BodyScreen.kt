package com.healthdiary.app.ui.screens.body

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
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

private val ANGLES = listOf("正面", "侧面", "背面")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(viewModel: BodyViewModel = viewModel()) {
    val metric by viewModel.metric.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val allMetrics by viewModel.allMetrics.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allPhotos.collectAsStateWithLifecycle()

    var weightInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var hipInput by remember { mutableStateOf("") }
    var photoAngle by remember { mutableStateOf<String?>(null) }
    var compareAngle by remember { mutableStateOf<String?>(null) }

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
                ANGLES.forEach { angle ->
                    val anglePhotos = photos.filter { it.angle == angle }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            angle,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (allPhotos.count { it.angle == angle } >= 2) {
                            IconButton(onClick = { compareAngle = angle }) {
                                Icon(Icons.Outlined.CompareArrows, contentDescription = "前后对比")
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
                        EmptyHint("还没有${angle}照片")
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            anglePhotos.forEach { photo ->
                                Box {
                                    AsyncImage(
                                        model = File(photo.filePath),
                                        contentDescription = "${angle}照片",
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.deletePhoto(photo) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(14.dp)
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
}

@Composable
private fun WeightChart(points: List<Pair<String, Float>>) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
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
                val topPad = 16.dp.toPx()
                val bottomPad = 24.dp.toPx()
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

                val path = Path()
                points.forEachIndexed { index, (_, weight) ->
                    val x = leftPad + step * index
                    val y = topPad + chartH * (1f - (weight - min) / span)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                points.forEachIndexed { index, (_, weight) ->
                    val x = leftPad + step * index
                    val y = topPad + chartH * (1f - (weight - min) / span)
                    drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }

                val maxText = "最高 ${max.toDisplayString()}kg"
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(maxText),
                    topLeft = Offset(leftPad, 0f),
                    style = labelStyle
                )
                val firstDate = points.first().first
                val firstDateWidth = textMeasurer.measure(
                    AnnotatedString(firstDate),
                    style = labelStyle
                ).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(firstDate),
                    topLeft = Offset(leftPad, size.height - 20.dp.toPx()),
                    style = labelStyle
                )
                val lastDate = points.last().first
                val lastDateWidth = textMeasurer.measure(
                    AnnotatedString(lastDate),
                    style = labelStyle
                ).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(lastDate),
                    topLeft = Offset(
                        size.width - rightPad - lastDateWidth,
                        size.height - 20.dp.toPx()
                    ),
                    style = labelStyle
                )
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
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(4.dp)
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
