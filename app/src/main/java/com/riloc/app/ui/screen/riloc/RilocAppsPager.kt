package com.riloc.app.ui.screen.riloc

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riloc.app.R
import com.riloc.app.common.Prefs
import com.riloc.app.ui.LocalUiMode
import com.riloc.app.ui.UiMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

private data class AppEntry(val label: String, val packageName: String, val isSystem: Boolean)

/** Riloc target-apps page (replaces the template's Module tab). */
@Composable
fun RilocAppsPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val allApps = remember {
        @Suppress("DEPRECATION")
        runCatching {
            pm.getInstalledApplications(0)
                .mapNotNull { info: ApplicationInfo ->
                    val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrNull() ?: info.packageName
                    AppEntry(label, info.packageName, (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }.getOrDefault(emptyList())
    }

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(Prefs.targetApps()) }

    fun toggle(pkg: String, checked: Boolean) {
        selected = if (checked) selected + pkg else selected - pkg
        Prefs.setTargetApps(selected)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.module),
                color = Color.Transparent,
            )
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                InputField(
                    query = query,
                    onQueryChange = { query = it },
                    label = stringResource(R.string.riloc_apps_search),
                    expanded = false,
                    onExpandedChange = {},
                    onSearch = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    stringResource(R.string.riloc_apps_selected, selected.size),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
                val filtered = allApps.filter {
                    query.isBlank() ||
                        it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.packageName }) { app ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { toggle(app.packageName, app.packageName !in selected) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (app.packageName in selected) Icons.Rounded.CheckCircle
                                    else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (app.packageName in selected) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        app.packageName,
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (app.isSystem) {
                                    Text(
                                        "系统",
                                        fontSize = 11.sp,
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
