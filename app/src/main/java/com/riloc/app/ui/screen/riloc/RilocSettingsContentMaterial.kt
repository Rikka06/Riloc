package com.riloc.app.ui.screen.riloc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riloc.app.R
import com.riloc.app.common.Prefs

/** Riloc location & anti-detection settings, Material flavor. */
@Composable
fun RilocSettingsCardMaterial() {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            stringResource(R.string.riloc_map_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        var mapStyle by remember { mutableStateOf(MapStyles.current()) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            MapStyle.entries.forEach { style ->
                val selected = mapStyle == style
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            mapStyle = style
                            MapStyles.setCurrent(style)
                        },
                ) {
                    Text(
                        style.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.riloc_location_params),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            MatSwitchRow(
                title = stringResource(R.string.riloc_accuracy),
                checked = remember { mutableStateOf(Prefs.useAccuracy()) },
                onChecked = { Prefs.setUseAccuracy(it) },
            )
            MatSliderRow(
                title = stringResource(R.string.riloc_accuracy),
                value = Prefs.accuracy().toFloat(),
                range = 1f..100f,
                format = { "%.1f m".format(it) },
                onChange = { Prefs.setAccuracy(it.toDouble()) },
            )
            MatSwitchRow(
                title = stringResource(R.string.riloc_altitude),
                checked = remember { mutableStateOf(Prefs.useAltitude()) },
                onChecked = { Prefs.setUseAltitude(it) },
            )
            MatSliderRow(
                title = stringResource(R.string.riloc_altitude),
                value = Prefs.altitude().toFloat(),
                range = -100f..3000f,
                format = { "%.0f m".format(it) },
                onChange = { Prefs.setAltitude(it.toDouble()) },
            )
            MatSwitchRow(
                title = stringResource(R.string.riloc_speed),
                checked = remember { mutableStateOf(Prefs.useSpeed()) },
                onChecked = { Prefs.setUseSpeed(it) },
            )
            MatSliderRow(
                title = stringResource(R.string.riloc_speed),
                value = Prefs.speed(),
                range = 0f..40f,
                format = { "%.1f m/s".format(it) },
                onChange = { Prefs.setSpeed(it) },
            )
            MatSwitchRow(
                title = stringResource(R.string.riloc_randomize),
                subtitle = stringResource(R.string.riloc_randomize_summary),
                checked = remember { mutableStateOf(Prefs.useRandomize()) },
                onChecked = { Prefs.setRandomize(it) },
            )
        }

        Text(
            stringResource(R.string.riloc_anti_detect),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            MatSwitchRow(title = stringResource(R.string.riloc_hide_mock), subtitle = stringResource(R.string.riloc_hide_mock_summary),
                checked = remember { mutableStateOf(Prefs.hideMockFlag()) }, onChecked = { Prefs.setHideMockFlag(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_normalize_provider), subtitle = stringResource(R.string.riloc_normalize_provider_summary),
                checked = remember { mutableStateOf(Prefs.normalizeProvider()) }, onChecked = { Prefs.setNormalizeProvider(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hook_vendor_sdks), subtitle = stringResource(R.string.riloc_hook_vendor_sdks_summary),
                checked = remember { mutableStateOf(Prefs.hookVendorSdks()) }, onChecked = { Prefs.setHookVendorSdks(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hook_nmea), subtitle = stringResource(R.string.riloc_hook_nmea_summary),
                checked = remember { mutableStateOf(Prefs.hookNmea()) }, onChecked = { Prefs.setHookNmea(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_packages), subtitle = stringResource(R.string.riloc_hide_packages_summary),
                checked = remember { mutableStateOf(Prefs.hidePackages()) }, onChecked = { Prefs.setHidePackages(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_stack_trace), subtitle = stringResource(R.string.riloc_hide_stack_trace_summary),
                checked = remember { mutableStateOf(Prefs.hideStackTrace()) }, onChecked = { Prefs.setHideStackTrace(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_simulate_sensors), subtitle = stringResource(R.string.riloc_simulate_sensors_summary),
                checked = remember { mutableStateOf(Prefs.simulateSensors()) }, onChecked = { Prefs.setSimulateSensors(it) })
            val context = androidx.compose.ui.platform.LocalContext.current
            MatSwitchRow(title = stringResource(R.string.riloc_floating_joystick), subtitle = stringResource(R.string.riloc_floating_joystick_summary),
                checked = remember { mutableStateOf(Prefs.floatingJoystick()) }, onChecked = { enabled ->
                    Prefs.setFloatingJoystick(enabled)
                    if (enabled) {
                        com.riloc.app.engine.FloatingJoystickService.start(context)
                    } else {
                        com.riloc.app.engine.FloatingJoystickService.stop(context)
                    }
                })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_appops), subtitle = stringResource(R.string.riloc_hide_appops_summary),
                checked = remember { mutableStateOf(Prefs.hideAppOps()) }, onChecked = { Prefs.setHideAppOps(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_settings), subtitle = stringResource(R.string.riloc_hide_settings_summary),
                checked = remember { mutableStateOf(Prefs.hideSettings()) }, onChecked = { Prefs.setHideSettings(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_wifi), subtitle = stringResource(R.string.riloc_hide_wifi_summary),
                checked = remember { mutableStateOf(Prefs.hideWifi()) }, onChecked = { Prefs.setHideWifi(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_telephony), subtitle = stringResource(R.string.riloc_hide_telephony_summary),
                checked = remember { mutableStateOf(Prefs.hideTelephony()) }, onChecked = { Prefs.setHideTelephony(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_enable_system_hooks), subtitle = stringResource(R.string.riloc_enable_system_hooks_summary),
                checked = remember { mutableStateOf(Prefs.enableSystemHooks()) }, onChecked = { Prefs.setEnableSystemHooks(it) })
            MatSwitchRow(title = stringResource(R.string.riloc_hide_toast), subtitle = stringResource(R.string.riloc_hide_toast_summary),
                checked = remember { mutableStateOf(Prefs.hideToast()) }, onChecked = { Prefs.setHideToast(it) })
        }
    }
}


@Composable
private fun MatSwitchRow(
    title: String,
    checked: androidx.compose.runtime.MutableState<Boolean>,
    onChecked: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    var local by remember { checked }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = local, onCheckedChange = { local = it; onChecked(it) })
    }
}

@Composable
private fun MatSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var local by remember { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(format(local), style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = local, onValueChange = { local = it; onChange(it) }, valueRange = range)
    }
}
