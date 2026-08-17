package com.asptechinc.daymark.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.asptechinc.daymark.config.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Sherida101/Daymark/releases/latest"

    data class GitHubRelease(
        val tag_name: String,
        val assets: List<GitHubAsset>,
    )

    data class GitHubAsset(
        val name: String,
        val browser_download_url: String,
    )

    suspend fun checkForUpdate(currentVersion: String): GitHubRelease? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val release = Gson().fromJson(response, GitHubRelease::class.java)

                    if (isNewerVersion(currentVersion, release.tag_name)) {
                        return@withContext release
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for update", e)
            }
            null
        }

    internal fun isNewerVersion(
        current: String,
        latest: String,
    ): Boolean {
        val currentClean = current.removePrefix("v").trim()
        val latestClean = latest.removePrefix("v").trim()

        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val curr = currentParts.getOrNull(i) ?: 0
            val lat = latestParts.getOrNull(i) ?: 0
            if (lat > curr) return true
            if (lat < curr) return false
        }
        return false
    }

    fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        fileName: String,
    ) {
        val request =
            DownloadManager
                .Request(Uri.parse(downloadUrl))
                .setTitle("Downloading Daymark Update")
                .setDescription("Version $fileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(context, fileName)
                        context.unregisterReceiver(this)
                    }
                }
            }
        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun installApk(
        context: Context,
        fileName: String,
    ) {
        val file =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName,
            )
        if (!file.exists()) {
            Log.e(TAG, "APK file not found: ${file.absolutePath}")
            return
        }

        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent =
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(context, AppConfig.FILE_PROVIDER_AUTHORITY, file)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }
}
