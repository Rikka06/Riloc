package com.riloc.app.ui.util.module

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.core.net.toUri

object Shortcut {
    private const val TAG = "ModuleShortcut"
    private const val TEMPLATE_MESSAGE = "UI template: shortcut actions are disabled."

    fun createModuleActionShortcut(
        context: Context,
        moduleId: String,
        name: String,
        iconUri: String?
    ) {
        Log.d(TAG, "createModuleActionShortcut mocked: moduleId=$moduleId, name=$name, iconUri=$iconUri")
        Toast.makeText(context, TEMPLATE_MESSAGE, Toast.LENGTH_SHORT).show()
    }

    fun createModuleWebUiShortcut(
        context: Context,
        moduleId: String,
        name: String,
        iconUri: String?
    ) {
        Log.d(TAG, "createModuleWebUiShortcut mocked: moduleId=$moduleId, name=$name, iconUri=$iconUri")
        Toast.makeText(context, TEMPLATE_MESSAGE, Toast.LENGTH_SHORT).show()
    }

    fun hasModuleActionShortcut(context: Context, moduleId: String): Boolean = false

    fun hasModuleWebUiShortcut(context: Context, moduleId: String): Boolean = false

    fun deleteModuleActionShortcut(context: Context, moduleId: String) {
        Log.d(TAG, "deleteModuleActionShortcut mocked: moduleId=$moduleId")
        Toast.makeText(context, TEMPLATE_MESSAGE, Toast.LENGTH_SHORT).show()
    }

    fun deleteModuleWebUiShortcut(context: Context, moduleId: String) {
        Log.d(TAG, "deleteModuleWebUiShortcut mocked: moduleId=$moduleId")
        Toast.makeText(context, TEMPLATE_MESSAGE, Toast.LENGTH_SHORT).show()
    }

    fun loadShortcutBitmap(context: Context, iconUri: String?): Bitmap? {
        if (iconUri.isNullOrBlank()) return null
        val uri = iconUri.toUri()
        if (uri.scheme.equals("su", ignoreCase = true)) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }?.let { rawBitmap ->
                val side = minOf(rawBitmap.width, rawBitmap.height)
                val square = Bitmap.createBitmap(
                    rawBitmap,
                    (rawBitmap.width - side) / 2,
                    (rawBitmap.height - side) / 2,
                    side,
                    side
                )
                if (side > 512) square.scale(512, 512) else square
            }
        } catch (t: Throwable) {
            Log.w(TAG, "loadShortcutBitmap mocked loader failed: ${t.message}", t)
            null
        }
    }
}

