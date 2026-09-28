package com.example.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.model.FileCategory
import com.example.model.ManagedFile
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AndroidActionController(private val context: Context) {

    private var isTorchOn: Boolean = false

    /**
     * Toggle or set device flashlight state.
     */
    fun toggleFlashlight(enable: Boolean? = null): Result<String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                val newState = enable ?: !isTorchOn
                cameraManager.setTorchMode(cameraId, newState)
                isTorchOn = newState
                val statusText = if (newState) "Flashlight turned ON" else "Flashlight turned OFF"
                Result.success(statusText)
            } else {
                Result.failure(Exception("Flashlight hardware not available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Open Phone Dialer with optional number.
     */
    fun openDialer(phoneNumber: String? = null): Result<String> {
        return try {
            val uri = if (phoneNumber.isNullOrBlank()) Uri.parse("tel:") else Uri.parse("tel:$phoneNumber")
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opening Phone dialer")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launch an Android application by common name or fallback to Play Store/Search.
     */
    fun openApp(appName: String): Result<String> {
        val lower = appName.lowercase()
        val intent: Intent? = when {
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("ফ্ল্যাশলাইট") -> {
                val result = toggleFlashlight()
                return result
            }
            lower.contains("dial") || lower.contains("phone") || lower.contains("call") || lower.contains("ফোন") -> {
                return openDialer()
            }
            lower.contains("chrome") -> {
                context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            }
            lower.contains("youtube") || lower.contains("ইউটিউব") -> {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            }
            lower.contains("camera") || lower.contains("ক্যামেরা") -> {
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            }
            lower.contains("gallery") || lower.contains("photo") || lower.contains("গ্যালারি") -> {
                Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                }
            }
            lower.contains("wifi") || lower.contains("wi-fi") -> {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            lower.contains("bluetooth") -> {
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            lower.contains("setting") || lower.contains("সেটিংস") -> {
                Intent(Settings.ACTION_SETTINGS)
            }
            lower.contains("maps") || lower.contains("ম্যাপ") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            }
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> {
                context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com"))
            }
            lower.contains("gmail") || lower.contains("mail") || lower.contains("ইমেইল") -> {
                context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                    ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL)
            }
            lower.contains("play store") || lower.contains("store") -> {
                context.packageManager.getLaunchIntentForPackage("com.android.vending")
            }
            lower.contains("alarm") || lower.contains("clock") || lower.contains("ঘড়ি") -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS)
            }
            else -> {
                val installed = context.packageManager.getInstalledApplications(0)
                val match = installed.firstOrNull { it.loadLabel(context.packageManager).toString().contains(appName, ignoreCase = true) }
                if (match != null) {
                    context.packageManager.getLaunchIntentForPackage(match.packageName)
                } else null
            }
        }

        return if (intent != null) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result.success("Launched $appName")
            } catch (e: Exception) {
                Result.failure(Exception("Could not launch $appName: ${e.message}"))
            }
        } else {
            Result.failure(Exception("Application $appName is not installed on this device."))
        }
    }

    /**
     * Perform web search using Android's system SearchManager / Browser Intent.
     */
    fun searchWeb(query: String): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Searching for: $query")
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Result.success("Searched web for: $query")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Open a web URL directly in browser.
     */
    fun openWebsite(url: String): Result<String> {
        val target = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened $target")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share text or content to other apps.
     */
    fun shareContent(title: String, text: String): Result<String> {
        return try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_TITLE, title)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success("Shared successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize sample files in app sandbox for demonstration of file management operations.
     */
    fun getInitialManagedFiles(): List<ManagedFile> {
        val filesDir = context.filesDir
        ensureSampleFilesExist(filesDir)

        return listOf(
            ManagedFile(
                name = "project_specs.pdf",
                size = "2.4 MB",
                category = FileCategory.DOCUMENTS,
                dateModified = "Today, 08:30 AM",
                path = File(filesDir, "project_specs.pdf").absolutePath
            ),
            ManagedFile(
                name = "assistant_demo.mp4",
                size = "128 MB",
                category = FileCategory.VIDEOS,
                dateModified = "Today, 07:15 AM",
                path = File(filesDir, "assistant_demo.mp4").absolutePath
            ),
            ManagedFile(
                name = "avatar_preview.jpg",
                size = "1.8 MB",
                category = FileCategory.IMAGES,
                dateModified = "Yesterday, 04:20 PM",
                path = File(filesDir, "avatar_preview.jpg").absolutePath
            ),
            ManagedFile(
                name = "system_backup.zip",
                size = "12.6 MB",
                category = FileCategory.ARCHIVES,
                dateModified = "Yesterday, 02:00 PM",
                path = File(filesDir, "system_backup.zip").absolutePath
            ),
            ManagedFile(
                name = "quarterly_report.pdf",
                size = "3.1 MB",
                category = FileCategory.DOCUMENTS,
                dateModified = "Sep 24, 2026",
                path = File(filesDir, "quarterly_report.pdf").absolutePath
            ),
            ManagedFile(
                name = "maya_wallpaper.png",
                size = "4.2 MB",
                category = FileCategory.IMAGES,
                dateModified = "Sep 22, 2026",
                path = File(filesDir, "maya_wallpaper.png").absolutePath
            )
        )
    }

    private fun ensureSampleFilesExist(dir: File) {
        try {
            val sample = File(dir, "project_specs.pdf")
            if (!sample.exists()) {
                sample.writeText("Maya Assistant Project Specifications - Confidential")
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Zip operation for native files.
     */
    fun zipFiles(targetName: String): Result<String> {
        return try {
            val zipFile = File(context.filesDir, "$targetName.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { out ->
                val entry = ZipEntry("readme.txt")
                out.putNextEntry(entry)
                out.write("Archived by Maya Voice Assistant".toByteArray())
                out.closeEntry()
            }
            Result.success("Created archive ${zipFile.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
