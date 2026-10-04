package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseTitle: String,
    val releaseNotes: String
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_REPO = "custom_github_repo"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getSavedRepo(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_REPO, "") ?: ""
        return if (saved.isNotBlank()) saved else BuildConfig.GITHUB_REPOSITORY
    }

    fun saveRepo(context: Context, repo: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REPO, repo.trim()).apply()
    }

    suspend fun checkUpdate(context: Context, targetRepo: String? = null): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val repo = targetRepo?.trim()?.takeIf { it.isNotBlank() } ?: getSavedRepo(context)
            if (repo.isBlank() || !repo.contains("/")) {
                Log.w(TAG, "No valid GitHub repository configured (format: owner/repo)")
                return@withContext UpdateInfo(false, "", 0, "", "", "")
            }

            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "NovelAI-TTS-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub release check failed: HTTP ${response.code}")
                    return@withContext UpdateInfo(false, "", 0, "", "", "")
                }
                val bodyStr = response.body?.string() ?: return@withContext UpdateInfo(false, "", 0, "", "", "")
                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "")
                val releaseTitle = json.optString("name", "NovelAI TTS Update")
                val releaseNotes = json.optString("body", "")

                // Find APK asset download URL
                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (downloadUrl.isBlank()) {
                    return@withContext UpdateInfo(false, "", 0, "", "", "")
                }

                // Parse version
                val cleanTag = tagName.removePrefix("v").trim()
                val remoteVerStr = cleanTag.substringBefore("-")
                val remoteBuildStr = if (cleanTag.contains("-b")) {
                    cleanTag.substringAfter("-b").substringBefore("-")
                } else ""
                val remoteBuildCode = remoteBuildStr.toIntOrNull() ?: 0

                val currentVer = BuildConfig.VERSION_NAME
                val currentCode = BuildConfig.VERSION_CODE

                val isNewer = if (remoteBuildCode > 0) {
                    remoteBuildCode > currentCode
                } else {
                    compareVersions(remoteVerStr, currentVer) > 0
                }

                return@withContext UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersionName = remoteVerStr.ifBlank { tagName },
                    latestVersionCode = remoteBuildCode,
                    downloadUrl = downloadUrl,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            UpdateInfo(false, "", 0, "", "", "")
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val contentLength = body.contentLength()

                val apkDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(apkDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            return@withContext false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install launch error", e)
        }
    }
}
