package com.example.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder

object DownloadHelper {
    private const val TAG = "DownloadHelper"

    private val _downloadsFlow = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadsFlow = _downloadsFlow.asStateFlow()

    private var isReceiverRegistered = false

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                context?.let { ctx ->
                    refreshDownloads(ctx)
                }
            }
        }
    }

    fun init(context: Context) {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.applicationContext.registerReceiver(
                        downloadCompleteReceiver,
                        filter,
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    context.applicationContext.registerReceiver(downloadCompleteReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.w(TAG, "Error registering download complete receiver: ${e.message}")
            }
        }
        refreshDownloads(context)
    }

    fun refreshDownloads(context: Context) {
        val files = mutableListOf<DownloadItem>()
        val seenPaths = mutableSetOf<String>()

        try {
            // 1. App-specific download folder
            val appDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (appDownloadDir != null && appDownloadDir.exists()) {
                appDownloadDir.listFiles()?.forEach { file ->
                    if (file.isFile && !seenPaths.contains(file.absolutePath)) {
                        seenPaths.add(file.absolutePath)
                        files.add(DownloadItem(fileName = file.name, file = file))
                    }
                }
            }

            // 2. Public Downloads folder (if accessible)
            val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloadDir != null && publicDownloadDir.exists()) {
                publicDownloadDir.listFiles()?.forEach { file ->
                    if (file.isFile && !seenPaths.contains(file.absolutePath)) {
                        // Include recently downloaded files or common novel/text formats
                        seenPaths.add(file.absolutePath)
                        files.add(DownloadItem(fileName = file.name, file = file))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning downloads: ${e.message}")
        }

        // Sort by newest first
        val sortedList = files.sortedByDescending { it.lastModified }
        _downloadsFlow.value = sortedList
    }

    fun downloadFromWeb(
        context: Context,
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimetype: String? = null,
        onDownloadStarted: ((fileName: String) -> Unit)? = null
    ): Boolean {
        try {
            var cleanFileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            try {
                cleanFileName = URLDecoder.decode(cleanFileName, "UTF-8")
            } catch (_: Exception) {}

            // Target destination directory: App-specific Download directory (Always accessible without runtime permissions)
            val appDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir

            val targetFile = File(appDownloadDir, cleanFileName)
            val uri = Uri.parse(url)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                val request = DownloadManager.Request(uri).apply {
                    setTitle(cleanFileName)
                    setDescription("กำลังดาวน์โหลดจากเว็ปไซต์...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationUri(Uri.fromFile(targetFile))
                    setMimeType(mimetype ?: DownloadItem.getMimeType(targetFile))

                    // Cookie forwarding
                    val cookies = CookieManager.getInstance().getCookie(url)
                    if (!cookies.isNullOrBlank()) {
                        addRequestHeader("Cookie", cookies)
                    }
                    if (!userAgent.isNullOrBlank()) {
                        addRequestHeader("User-Agent", userAgent)
                    }
                }

                downloadManager.enqueue(request)
                onDownloadStarted?.invoke(cleanFileName)
                Toast.makeText(context, "เริ่มดาวน์โหลด: $cleanFileName", Toast.LENGTH_SHORT).show()
                refreshDownloads(context)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting download: ${e.message}", e)
            Toast.makeText(context, "ไม่สามารถเริ่มดาวน์โหลดได้: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return false
    }

    fun openFile(
        context: Context,
        item: DownloadItem
    ) {
        try {
            if (!item.file.exists()) {
                Toast.makeText(context, "ไม่พบไฟล์ (อาจถูกลบไปแล้ว)", Toast.LENGTH_SHORT).show()
                refreshDownloads(context)
                return
            }

            // Standard Open with Android system app / chooser via FileProvider (Google Chrome style)
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )

            val mime = item.mimeType.ifBlank { "*/*" }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "เปิดไฟล์: ${item.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file: ${e.message}", e)
            Toast.makeText(context, "ไม่พบแอปที่รองรับการเปิดไฟล์นี้", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, item: DownloadItem) {
        try {
            if (!item.file.exists()) {
                Toast.makeText(context, "ไม่พบไฟล์", Toast.LENGTH_SHORT).show()
                return
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, item.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "แชร์ไฟล์").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}", e)
            Toast.makeText(context, "ไม่สามารถแชร์ไฟล์ได้: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun deleteFile(context: Context, item: DownloadItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (item.file.exists()) {
                val deleted = item.file.delete()
                refreshDownloads(context)
                return@withContext deleted
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}", e)
        }
        return@withContext false
    }

    suspend fun readFileContent(file: File): String = withContext(Dispatchers.IO) {
        try {
            if (file.exists() && file.canRead()) {
                return@withContext file.readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file content: ${e.message}", e)
        }
        return@withContext ""
    }
}
