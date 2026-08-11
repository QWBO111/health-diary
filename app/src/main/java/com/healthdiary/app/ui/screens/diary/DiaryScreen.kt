package com.healthdiary.app.ui.screens.diary

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.data.local.DiaryMediaEntity
import com.healthdiary.app.ui.components.DateSelector
import com.healthdiary.app.ui.components.SectionCard
import com.healthdiary.app.util.Dates
import kotlinx.coroutines.delay
import java.io.File

private val EMOJIS = listOf("😞", "😕", "😐", "🙂", "😄")

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
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
        topBar = { TopAppBar(title = { Text("日记") }) }
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EMOJIS.forEachIndexed { index, emoji ->
                        FilterChip(
                            selected = viewModel.moodScore == index + 1,
                            onClick = { viewModel.setMood(index + 1) },
                            label = {
                                Text(
                                    emoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                }
            }

            SectionCard("日记内容") {
                OutlinedTextField(
                    value = viewModel.text,
                    onValueChange = { viewModel.text = it },
                    label = { Text("记录今天发生的事、想法、感受…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                )
            }

            SectionCard("照片") {
                if (photos.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        photos.forEach { media ->
                            Box {
                                AsyncImage(
                                    model = File(media.filePath),
                                    contentDescription = "日记照片",
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.deleteMedia(media) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "删除照片",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
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
                        }
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
                        recordingError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (isRecording) {
                    Button(
                        onClick = { stopRecording() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("停止录音 ${formatRecordTime(recordElapsed)}")
                    }
                } else {
                    Button(
                        onClick = {
                            stopPlayer()
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("开始录音")
                    }
                }
                if (audios.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    audios.forEach { media ->
                        AudioRow(
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

            Button(
                onClick = { viewModel.saveEntry() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存日记")
            }
        }
    }
}

@Composable
private fun AudioRow(
    media: DiaryMediaEntity,
    playing: Boolean,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlayToggle) {
            Icon(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放"
            )
        }
        Text(
            "语音 ${if (media.durationSec > 0) "${media.durationSec}秒" else ""}",
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "删除语音")
        }
    }
}

private fun formatRecordTime(millis: Long): String {
    val totalSec = millis / 1000
    return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
}
