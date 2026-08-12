package com.healthdiary.app.ui.screens.diary

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.DiaryMediaEntity
import com.healthdiary.app.ui.components.DateSelector
import com.healthdiary.app.ui.components.SectionCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private data class MoodOption(val score: Int, val emoji: String, val label: String)

private val MOODS = listOf(
    MoodOption(1, "😥", "糟糕"),
    MoodOption(2, "😞", "低落"),
    MoodOption(3, "😐", "平静"),
    MoodOption(4, "🙂", "开心"),
    MoodOption(5, "😄", "超棒")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(viewModel: DiaryViewModel = viewModel()) {
    val context = LocalContext.current
    val mediaStore = (context.applicationContext as HealthDiaryApp).container.mediaStore
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val photos = entry?.media?.filter { it.type == "photo" }.orEmpty()
    val audios = entry?.media?.filter { it.type == "audio" }.orEmpty()

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordElapsed by remember { mutableLongStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var recordingError by remember { mutableStateOf<String?>(null) }
    var playingPath by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var viewPhotoPath by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun startRecording() {
        val file = mediaStore.newAudioFile()
        val recorderInstance = runCatching {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.getOrElse {
            recordingError = "录音启动失败：${it.message}"
            return
        }
        recorder = recorderInstance
        recordFile = file
        recordElapsed = 0L
        isRecording = true
        recordingError = null
    }

    fun stopRecording() {
        val r = recorder ?: return
        val file = recordFile
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        isRecording = false
        if (file != null && file.exists() && file.length() > 0) {
            viewModel.addAudio(file.absolutePath, (recordElapsed / 1000).toInt())
        }
        recordElapsed = 0L
    }

    fun stopPlayer() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        playingPath = null
    }

    fun play(path: String) {
        if (playingPath == path) {
            stopPlayer()
            return
        }
        stopPlayer()
        val instance = runCatching {
            MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { stopPlayer() }
                prepare()
                start()
            }
        }.getOrNull()
        if (instance != null) {
            player = instance
            playingPath = path
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addPhoto(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { viewModel.addPhoto(it) }
        }
        cameraUri = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            recordingError = "需要麦克风权限才能录音"
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordElapsed += 1000
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop() }
            recorder?.release()
            player?.release()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("日记") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        viewModel.saveEntry {
                            scope.launch {
                                snackbarHostState.showSnackbar("日记已保存 ✓")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("保存日记")
                }
            }
        }
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

            SectionCard("今天的心情") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MOODS.forEach { mood ->
                        MoodCard(
                            option = mood,
                            selected = viewModel.moodScore == mood.score,
                            onClick = { viewModel.setMood(mood.score) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionCard("日记内容") {
                OutlinedTextField(
                    value = viewModel.text,
                    onValueChange = { viewModel.text = it },
                    placeholder = { Text("记录今天发生的事、想法、感受…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "已写 ${viewModel.text.length} 字",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            SectionCard("照片") {
                if (photos.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🖼️", fontSize = 28.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "还没有照片，记录下今天的瞬间吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    photos.chunked(2).forEach { rowPhotos ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPhotos.forEach { media ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewPhotoPath = media.filePath }
                                ) {
                                    AsyncImage(
                                        model = File(media.filePath),
                                        contentDescription = "日记照片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteMedia(media) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(30.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "删除照片",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (rowPhotos.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("从相册选择")
                    }
                    OutlinedButton(
                        onClick = {
                            val file = mediaStore.tempPhotoFile()
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("拍照")
                    }
                }
            }

            SectionCard("语音") {
                if (recordingError != null) {
                    Text(
                        text = recordingError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (isRecording) {
                    Button(
                        onClick = { stopRecording() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("停止录音 ${formatRecordTime(recordElapsed)}")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            stopPlayer()
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("开始录音")
                    }
                }

                if (audios.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "还没有语音记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    audios.forEach { media ->
                        AudioCard(
                            media = media,
                            playing = playingPath == media.filePath,
                            onPlayToggle = { play(media.filePath) },
                            onDelete = {
                                if (playingPath == media.filePath) stopPlayer()
                                viewModel.deleteMedia(media)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    viewPhotoPath?.let { path ->
        PhotoViewerDialog(path = path, onDismiss = { viewPhotoPath = null })
    }
}

@Composable
private fun MoodCard(
    option: MoodOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        label = "moodScale"
    )
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(option.emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun AudioCard(
    media: DiaryMediaEntity,
    playing: Boolean,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (playing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
            ) {
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (playing) "暂停" else "播放",
                    tint = if (playing) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (playing) "正在播放" else "语音录音",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Normal
                )
                if (media.durationSec > 0) {
                    Text(
                        text = formatRecordTime(media.durationSec * 1000L),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除语音",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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

private fun formatRecordTime(millis: Long): String {
    val totalSec = millis / 1000
    return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
}
