package com.riloc.app.ui.screen.riloc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riloc.app.R
import com.riloc.app.common.Prefs
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Riloc location & anti-detection settings card, Miuix flavor.
 */
@Composable
fun RilocSettingsCardMiuix() {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.defaultColors(color = colorScheme.surface),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("高德地图 API Key & 安全密钥配置", style = TextStyle(fontSize = 14.sp, color = colorScheme.primary))
                Spacer(Modifier.height(8.dp))
                
                var amapKeyStr by remember { mutableStateOf(Prefs.amapKey()) }
                Text("高德 Web 服务 Key", style = TextStyle(fontSize = 12.sp, color = colorScheme.onSurface))
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = amapKeyStr,
                    onValueChange = {
                        amapKeyStr = it
                        Prefs.setAmapKey(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入高德 Web 服务 Key", fontSize = 12.sp, color = colorScheme.onSurfaceVariantSummary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(10.dp))

                var amapSecretStr by remember { mutableStateOf(Prefs.amapSecret()) }
                Text("高德安全密钥 (Security Secret)", style = TextStyle(fontSize = 12.sp, color = colorScheme.onSurface))
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = amapSecretStr,
                    onValueChange = {
                        amapSecretStr = it
                        Prefs.setAmapSecret(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入高德安全密钥 (Secret Code)", fontSize = 12.sp, color = colorScheme.onSurfaceVariantSummary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(6.dp))
                Text("内置官方高额度 Key 已开启，亦可填写个人 Key 与安全密钥享受专属图层与搜索", style = TextStyle(fontSize = 11.sp, color = colorScheme.onSurfaceVariantSummary))
            }
        }


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.defaultColors(color = colorScheme.surface),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    stringResource(R.string.riloc_map_style),
                    style = TextStyle(fontSize = 14.sp, color = colorScheme.primary),
                )
                Spacer(Modifier.height(6.dp))
                var mapStyle by remember { mutableStateOf(MapStyles.current()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MapStyle.entries.forEach { style ->
                        val selected = mapStyle == style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) colorScheme.primary else colorScheme.surfaceContainerHigh)
                                .clickable {
                                    mapStyle = style
                                    MapStyles.setCurrent(style)
                                },
                        ) {
                            Text(
                                style.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                color = if (selected) colorScheme.onPrimary else colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }

        Text(
            stringResource(R.string.riloc_location_params),
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
            style = TextStyle(
                fontSize = 14.sp,
                color = colorScheme.primary,
            ),
        )

        ValueSliderRow(
            title = stringResource(R.string.riloc_accuracy),
            value = Prefs.accuracy().toFloat(),
            range = 1f..100f,
            format = { "%.1f m".format(it) },
            icon = { Icon(Icons.Rounded.GpsFixed, contentDescription = null, tint = colorScheme.onBackground) },
        ) { Prefs.setAccuracy(it.toDouble()) }

        ValueSliderRow(
            title = stringResource(R.string.riloc_altitude),
            value = Prefs.altitude().toFloat(),
            range = -100f..3000f,
            format = { "%.0f m".format(it) },
            icon = { Icon(Icons.Rounded.Height, contentDescription = null, tint = colorScheme.onBackground) },
        ) { Prefs.setAltitude(it.toDouble()) }

        ValueSliderRow(
            title = stringResource(R.string.riloc_speed),
            value = Prefs.speed(),
            range = 0f..40f,
            format = { "%.1f m/s".format(it) },
            icon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = colorScheme.onBackground) },
        ) { Prefs.setSpeed(it) }

        SwitchPreference(
            title = "隐藏模拟定位标志",
            summary = "屏蔽 Settings.Secure.ALLOW_MOCK_LOCATION 检测",
            startAction = {
                Icon(
                    Icons.Rounded.VisibilityOff,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = "隐藏模拟定位标志",
                    tint = colorScheme.onBackground,
                )
            },
            checked = Prefs.hideMockFlag(),
            onCheckedChange = { Prefs.setHideMockFlag(it) },
        )

        SwitchPreference(
            title = "高德/百度/腾讯地图 SDK 拦截",
            summary = "拦截地图厂商 SDK 反作弊坐标定位查询",
            startAction = {
                Icon(
                    Icons.Rounded.Security,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = "SDK 拦截",
                    tint = colorScheme.onBackground,
                )
            },
            checked = Prefs.hookVendorSdks(),
            onCheckedChange = { Prefs.setHookVendorSdks(it) },
        )
    }
}

@Composable
private fun ValueSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    icon: @Composable () -> Unit,
    onChange: (Float) -> Unit,
) {
    var v by remember(value) { mutableStateOf(value) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.defaultColors(color = colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontSize = 14.sp, color = colorScheme.onSurface)
                }
                Text(format(v), fontSize = 13.sp, color = colorScheme.primary)
            }
            Slider(
                value = v,
                onValueChange = {
                    v = it
                    onChange(it)
                },
                valueRange = range,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
