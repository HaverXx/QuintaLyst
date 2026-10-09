package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil3.imageLoader
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object IconUtils {
    private const val CUSTOM_ICON_PREFIX = "custom_app_icon"

    fun setIcon(context: Context, enabled: Boolean) {
        val pm = context.packageManager
        val dynamic = ComponentName(context, "com.metrolist.music.MainActivityAlias")
        val static = ComponentName(context, "com.metrolist.music.MainActivityStatic")

        pm.setComponentEnabledSetting(
            dynamic,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            static,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun getCustomIconFile(context: Context, path: String? = null): File? {
        if (path != null) {
            if (path.isBlank()) return null
            val file = File(path)
            return if (file.exists()) file else null
        }
        val found = context.filesDir.listFiles { _, name ->
            name.startsWith(CUSTOM_ICON_PREFIX) && name.endsWith(".png")
        }?.maxByOrNull { it.lastModified() }
        if (found != null && found.exists()) return found

        val legacy = File(context.filesDir, "custom_app_icon.png")
        return if (legacy.exists()) legacy else null
    }

    fun getCustomIconFile(context: Context): File {
        return getCustomIconFile(context, null) ?: File(context.filesDir, "custom_app_icon.png")
    }

    fun saveCustomIconFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            // Center-crop to square and scale to max 512x512
            val minDim = min(original.width, original.height)
            val cropX = (original.width - minDim) / 2
            val cropY = (original.height - minDim) / 2
            val square = Bitmap.createBitmap(original, cropX, cropY, minDim, minDim)
            val targetSize = min(512, minDim)
            val scaled = if (minDim != targetSize) {
                Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
            } else {
                square
            }

            // Clean up any old custom icon files to prevent clutter and cache collisions
            deleteCustomIconFiles(context)

            // Save with a unique timestamp in the filename so Coil and preferences never hit stale cache
            val newFile = File(context.filesDir, "${CUSTOM_ICON_PREFIX}_${System.currentTimeMillis()}.png")
            FileOutputStream(newFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Invalidate Coil cache
            try {
                context.imageLoader.memoryCache?.clear()
            } catch (_: Throwable) {}

            newFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteCustomIconFiles(context: Context) {
        val legacy = File(context.filesDir, "custom_app_icon.png")
        if (legacy.exists()) legacy.delete()
        context.filesDir.listFiles { _, name ->
            name.startsWith(CUSTOM_ICON_PREFIX) && name.endsWith(".png")
        }?.forEach { it.delete() }
    }

    fun deleteCustomIcon(context: Context): Boolean {
        deleteCustomIconFiles(context)
        try {
            context.imageLoader.memoryCache?.clear()
        } catch (_: Throwable) {}
        return true
    }

    fun addCustomIconShortcut(context: Context, title: String, path: String? = null): Boolean {
        return try {
            val file = getCustomIconFile(context, path) ?: return false
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false

            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return false
            }

            val icon = IconCompat.createWithBitmap(bitmap)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

            val appTitle = title.ifBlank { context.getString(R.string.app_name) }
            val shortcut = ShortcutInfoCompat.Builder(context, "custom_launcher_shortcut")
                .setShortLabel(appTitle)
                .setLongLabel(appTitle)
                .setIcon(icon)
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

