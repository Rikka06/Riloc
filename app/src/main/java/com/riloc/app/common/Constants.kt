package com.riloc.app.common

// ── App ──────────────────────────────────────────────────────────────
const val APP_PACKAGE = "com.riloc.app"
const val SHARED_PREFS_FILE = "riloc_prefs"
/** LSPosed remote-preferences group shared between the manager app and the Xposed module. */
const val REMOTE_PREFS_GROUP = "settings"

// ── Shared preference keys (written by the app, read by the module) ──
const val KEY_IS_PLAYING = "is_playing"
const val KEY_LATITUDE = "latitude"          // Double encoded as raw long bits
const val KEY_LONGITUDE = "longitude"        // Double encoded as raw long bits
const val KEY_USE_ACCURACY = "use_accuracy"
const val KEY_ACCURACY = "accuracy"          // Double (raw bits), meters
const val KEY_USE_ALTITUDE = "use_altitude"
const val KEY_ALTITUDE = "altitude"          // Double (raw bits), meters
const val KEY_USE_SPEED = "use_speed"
const val KEY_SPEED = "speed"                // Float, m/s
const val KEY_USE_VERTICAL_ACCURACY = "use_vertical_accuracy"
const val KEY_VERTICAL_ACCURACY = "vertical_accuracy"      // Float
const val KEY_USE_SPEED_ACCURACY = "use_speed_accuracy"
const val KEY_SPEED_ACCURACY = "speed_accuracy"            // Float
const val KEY_USE_RANDOMIZE = "use_randomize"
const val KEY_RANDOMIZE_RADIUS = "randomize_radius"        // Double (raw bits), meters
const val KEY_TARGET_APPS = "target_apps"    // comma-separated package names

// ── Anti-detection toggles ───────────────────────────────────────────
const val KEY_HIDE_MOCK_FLAG = "hide_mock_flag"       // isMock()/isFromMockProvider()/extras/mask
const val KEY_NORMALIZE_PROVIDER = "normalize_provider" // getProvider() -> gps/network
const val KEY_HIDE_APP_OPS = "hide_app_ops"           // AppOpsManager mock_location checks
const val KEY_HIDE_SETTINGS = "hide_settings"         // Settings.Secure mock_location
const val KEY_HIDE_WIFI = "hide_wifi"                 // WifiManager scan results
const val KEY_HIDE_TELEPHONY = "hide_telephony"       // com.android.phone cell info
const val KEY_ENABLE_SYSTEM_HOOKS = "enable_system_hooks" // system_server deep hooks
const val KEY_HIDE_TOAST = "hide_toast"
const val KEY_HOOK_VENDOR_SDKS = "hook_vendor_sdks"   // Baidu/AMap/Tencent map SDK hooks
const val KEY_HOOK_NMEA = "hook_nmea"                 // NMEA raw telemetry spoofing/blocking
const val KEY_HIDE_PACKAGES = "hide_packages"         // Conceal Riloc/LSPosed/Magisk from getInstalledPackages
const val KEY_HIDE_STACK_TRACE = "hide_stack_trace"   // Strip Xposed stack frames from Throwable.getStackTrace
const val KEY_SIMULATE_SENSORS = "simulate_sensors"   // Simulate step counter & accelerometer motion
const val KEY_FLOATING_JOYSTICK = "floating_joystick" // Enable system floating joystick window overlay


// ── Map ──────────────────────────────────────────────────────────────
const val KEY_MAP_STYLE = "map_style" // amap / amap_sat / carto / osm

// ── Movement mode (UI only, drives which engine writes lat/lon) ──────
const val MODE_STATIC = 0
const val MODE_JOYSTICK = 1

// ── Defaults ─────────────────────────────────────────────────────────
const val DEFAULT_LATITUDE = 39.9042          // Beijing
const val DEFAULT_LONGITUDE = 116.4074
const val DEFAULT_ACCURACY = 8.0
const val DEFAULT_ALTITUDE = 50.0
const val DEFAULT_SPEED = 0.0f
const val DEFAULT_VERTICAL_ACCURACY = 3.0f
const val DEFAULT_SPEED_ACCURACY = 0.3f
const val DEFAULT_RANDOMIZE_RADIUS = 50.0
const val DEFAULT_MAP_ZOOM = 17.0

/** Default scope hints shown in the About screen. */
val SYSTEM_HOOK_PACKAGES = listOf("system", "com.android.phone")
