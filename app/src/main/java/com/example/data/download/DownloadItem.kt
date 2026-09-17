package com.example.data.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory(val label: String) {
    ALL("ทั้งหมด"),
    NOVEL("นิยาย/ข้อความ"),
    DOCUMENT("เอกสาร/PDF"),
    AUDIO("ไฟล์เสียง"),
    IMAGE("รูปภาพ"),
    ARCHIVE("ไฟล์บีบอัด"),
    OTHER("อื่นๆ")
}

data class DownloadItem(
    val id: Long = System.currentTimeMillis(),
    val fileName: String,
    val file: File,
    val fileSize: Long = file.length(),
    val mimeType: String = getMimeType(file),
    val lastModified: Long = file.lastModified(),
    val category: FileCategory = determineCategory(file, mimeType)
) {
    val formattedSize: String
        get() {
            val bytes = fileSize
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
                else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("th", "TH"))
            return sdf.format(Date(lastModified))
        }

    val isNovelOrText: Boolean
        get() {
            val ext = file.extension.lowercase()
            return ext in listOf("txt", "epub", "md", "html", "htm", "json", "log", "rtf")
        }

    companion object {
        fun getMimeType(file: File): String {
            val extension = file.extension.lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
                "txt", "log", "md" -> "text/plain"
                "epub" -> "application/epub+zip"
                "pdf" -> "application/pdf"
                "mp3" -> "audio/mpeg"
                "m4a", "aac" -> "audio/mp4"
                "wav" -> "audio/wav"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                else -> "*/*"
            }
        }

        fun determineCategory(file: File, mimeType: String): FileCategory {
            val ext = file.extension.lowercase()
            val mime = mimeType.lowercase()
            return when {
                ext in listOf("txt", "epub", "md", "rtf") -> FileCategory.NOVEL
                ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") || mime.contains("pdf") || mime.contains("document") -> FileCategory.DOCUMENT
                ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac") || mime.startsWith("audio/") -> FileCategory.AUDIO
                ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg") || mime.startsWith("image/") -> FileCategory.IMAGE
                ext in listOf("zip", "rar", "7z", "tar", "gz") || mime.contains("zip") || mime.contains("compressed") -> FileCategory.ARCHIVE
                else -> FileCategory.OTHER
            }
        }
    }
}
