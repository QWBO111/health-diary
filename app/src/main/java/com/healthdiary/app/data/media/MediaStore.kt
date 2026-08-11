package com.healthdiary.app.data.media

import android.content.Context
import android.net.Uri
import java.io.File

class MediaStore(private val context: Context) {

    val mediaRoot: File
        get() = File(context.filesDir, "media")

    private val photosRoot: File
        get() = File(mediaRoot, "photos")

    private val audioRoot: File
        get() = File(mediaRoot, "audio")

    fun copyPhotoFromUri(uri: Uri, subFolder: String = ""): String {
        val dir = if (subFolder.isBlank()) photosRoot else File(photosRoot, subFolder)
        dir.mkdirs()
        val file = File(dir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取所选文件")
        return file.absolutePath
    }

    fun copyAudioFromUri(uri: Uri): String {
        audioRoot.mkdirs()
        val file = File(audioRoot, "rec_${System.currentTimeMillis()}_${(1000..9999).random()}.m4a")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取所选文件")
        return file.absolutePath
    }

    fun newAudioFile(): File {
        audioRoot.mkdirs()
        return File(audioRoot, "rec_${System.currentTimeMillis()}.m4a")
    }

    fun tempPhotoFile(): File {
        val dir = File(context.cacheDir, "camera")
        dir.mkdirs()
        return File(dir, "camera_${System.currentTimeMillis()}.jpg")
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    fun allMediaFiles(): List<File> {
        val result = mutableListOf<File>()
        fun walk(dir: File) {
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) walk(f) else result.add(f)
            }
        }
        if (mediaRoot.exists()) walk(mediaRoot)
        return result
    }

    fun relativeKey(path: String): String =
        "media/" + path.removePrefix(mediaRoot.absolutePath + File.separator).replace('\\', '/')
}
