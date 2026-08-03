package com.riloc.app.ui.kernelFlash.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.riloc.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 妲戒綅閫夋嫨瀵硅瘽妗嗙粍浠?
 * 鐢ㄤ簬Kernel鍒峰啓鏃堕€夋嫨鐩爣妲戒綅
 */
@Composable
fun SlotSelectionDialogMiuix(
    show: Boolean,
    onDismiss: () -> Unit,
    onSlotSelected: (String) -> Unit
) {
    var currentSlot by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    val showDialog = remember { mutableStateOf(show) }
    val operationFailedString = stringResource(R.string.operation_failed)

    LaunchedEffect(show) {
        showDialog.value = show
        if (show) {
            try {
                currentSlot = withContext(Dispatchers.IO) { getCurrentSlot() }
                // 璁剧疆榛樿閫夋嫨涓哄綋鍓嶆Ы浣?
                selectedSlot = when (currentSlot) {
                    "a" -> "a"
                    "b" -> "b"
                    else -> null
                }
                errorMessage = null
            } catch (_: Exception) {
                errorMessage = operationFailedString
                currentSlot = null
            }
        }
    }

    OverlayDialog(
        show = showDialog.value,
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = {
            showDialog.value = false
            onDismiss()
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                // 鏍囬
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    text = stringResource(id = R.string.select_slot_title),
                    fontSize = MiuixTheme.textStyles.title4.fontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface
                )

                // 褰撳墠妲戒綅鎴栭敊璇俊鎭?
                if (errorMessage != null) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        text = errorMessage ?: stringResource(R.string.operation_failed),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        text = stringResource(
                            id = R.string.current_slot,
                            currentSlot?.uppercase() ?: stringResource(R.string.not_supported)
                        ),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center
                    )
                }

                // 鎻忚堪鏂囨湰
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    text = stringResource(id = R.string.select_slot_description),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 妲戒綅閫夐」
                val slotOptions = listOf(
                    SlotOption(
                        slot = "a",
                        titleText = stringResource(id = R.string.slot_a),
                        icon = Icons.Filled.SdStorage
                    ),
                    SlotOption(
                        slot = "b",
                        titleText = stringResource(id = R.string.slot_b),
                        icon = Icons.Filled.SdStorage
                    )
                )

                slotOptions.forEach { option ->
                    ArrowPreference(
                        title = option.titleText,
                        startAction = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (selectedSlot == option.slot) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.onSurfaceVariantSummary
                                }
                            )
                        },
                        onClick = {
                            selectedSlot = option.slot
                        },
                        insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 鎸夐挳琛?
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = {
                            showDialog.value = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            selectedSlot?.let { onSlotSelected(it) }
                            showDialog.value = false
                            onDismiss()
                        },
                        enabled = selectedSlot != null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}


