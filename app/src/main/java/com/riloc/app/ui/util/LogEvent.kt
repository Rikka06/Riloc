package com.riloc.app.ui.util

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getBugreportFile(context: Context): File {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
    val current = LocalDateTime.now().format(formatter)
    val targetFile = File(context.cacheDir, "SukiSU_UI_Template_bugreport_$current.txt")
    targetFile.writeText(
        """
        SukiSU Ultra UI Template
        This placeholder bugreport is intentionally redacted.

        Kernel: 5.15.148-template-gki
        Device: Template Device
        Fingerprint: vendor/template/device:XX/TEMPLATE/000000:user/release-keys
        Logs: mock entries only
        """.trimIndent()
    )
    return targetFile
}

