package com.riloc.app.ui.screen.riloc

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.riloc.app.common.BookmarkManager
import com.riloc.app.common.CoordinateConverter
import com.riloc.app.common.HistoryManager
import com.riloc.app.common.Prefs
import com.riloc.app.engine.LatLng
import com.riloc.app.engine.LocationHub
import com.riloc.app.engine.MoveEngine
import com.riloc.app.engine.RouteEngine
import com.riloc.app.ui.components.Joystick
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

enum class MapMode { STATIC, JOYSTICK, ROUTE }


/**
 * High-definition AutoNavi AMap JS API v2.0 WebView screen aligned with GlobalTraveling / Shadow v1.2.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RilocMapPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
    engine: MoveEngine,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val current by LocationHub.current.collectAsState()
    val routeEngine = remember { RouteEngine(scope) }

    var isPlaying by remember { mutableStateOf(Prefs.isPlaying()) }
    var currentMode by remember { mutableStateOf(MapMode.STATIC) }
    var joystickSpeed by remember { mutableStateOf(Prefs.uiFloat("joystick_speed", 4f)) }
    var mapStyle by remember { mutableStateOf(MapStyles.current()) }
    var showStyleSelector by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var addressName by remember { mutableStateOf("正在获取位置名称…") }

    val routeWaypoints = remember { mutableStateListOf<LatLng>() }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    val webAppInterface = remember {
        WebAppInterface(
            onMapReadyCallback = {
                isMapReady = true
            },
            onPointSelectedCallback = { lat, lng, name ->
                scope.launch {
                    addressName = ReverseGeocoder.getAddress(context, lat, lng)
                }
            },
            onReceiveTipsCallback = { tipsStr ->
                if (tipsStr.isNotBlank()) {
                    val list = mutableListOf<SearchResult>()
                    tipsStr.split("|").forEach { tip ->
                        val parts = tip.split(",")
                        if (parts.size >= 4) {
                            val name = parts[0]
                            val district = parts[1]
                            val lat = parts[2].toDoubleOrNull() ?: return@forEach
                            val lon = parts[3].toDoubleOrNull() ?: return@forEach
                            list.add(SearchResult(name = name, address = district.ifBlank { name }, lat = lat, lon = lon))
                        }
                    }
                    searchSuggestions = list
                }
            },
            onPathDrawnCallback = { pathStr ->
                if (pathStr.isNotBlank()) {
                    val points = mutableListOf<LatLng>()
                    pathStr.split("|").forEach { pt ->
                        val coords = pt.split(",")
                        if (coords.size == 2) {
                            val lat = coords[0].toDoubleOrNull() ?: return@forEach
                            val lon = coords[1].toDoubleOrNull() ?: return@forEach
                            points.add(LatLng(lat, lon))
                        }
                    }
                    routeWaypoints.clear()
                    routeWaypoints.addAll(points)
                    routeEngine.setWaypoints(points)
                }
            },
            onStatusCallback = { status ->
                addressName = "[Riloc] $status"
                if (status.startsWith("ERR:") || status.startsWith("FALLBACK:")) {
                    Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Reverse geocode address when coordinates update
    LaunchedEffect(current) {
        scope.launch {
            addressName = ReverseGeocoder.getAddress(context, current.lat, current.lon)
        }
        webViewRef?.evaluateJavascript("jsSetStart(${current.lat}, ${current.lon}, '当前位置');", null)
    }

    // Query live search suggestions as user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            webViewRef?.evaluateJavascript("jsFetchTips('${searchQuery.replace("'", "\\'")}');", null)
        } else {
            searchSuggestions = emptyList()
        }
    }

    // Handle map drawing mode toggling
    LaunchedEffect(currentMode) {
        val enableDraw = (currentMode == MapMode.ROUTE)
        webViewRef?.evaluateJavascript("jsSetDrawingMode($enableDraw);", null)
    }

    // Sensor compass heading listener for live location arrow orientation
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    DisposableEffect(Unit) {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ORIENTATION) {
                    val azimuth = event.values[0]
                    webViewRef?.evaluateJavascript("jsSetHeading($azimuth);", null)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val performSearch = {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            keyboardController?.hide()
            scope.launch {
                val coords = LocationSearchHelper.searchLocation(context, searchQuery)
                isSearching = false
                searchSuggestions = emptyList()
                if (coords != null) {
                    LocationHub.update(coords.first, coords.second)
                    HistoryManager.add(searchQuery, coords.first, coords.second)
                    webViewRef?.evaluateJavascript("jsSetStart(${coords.first}, ${coords.second}, '${searchQuery}');", null)
                    Toast.makeText(context, "已定位至：$searchQuery", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未找到结果：$searchQuery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val locateRealDevice = {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val updateLoc = { loc: Location ->
            val (gcjLat, gcjLng) = CoordinateConverter.wgs84ToGcj02(loc.latitude, loc.longitude)
            LocationHub.update(gcjLat, gcjLng)
            HistoryManager.add("真实位置", gcjLat, gcjLng)
            webViewRef?.evaluateJavascript("jsSetStart(${gcjLat}, ${gcjLng}, '真实位置');", null)
            Toast.makeText(context, "已精准定位至当前设备真实位置", Toast.LENGTH_SHORT).show()
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                updateLoc(loc)
                lm?.removeUpdates(this)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        val hasGps = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val hasNetwork = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (hasGps || hasNetwork) {
            val provider = if (hasNetwork) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
            runCatching {
                lm?.requestLocationUpdates(provider, 1000L, 0f, listener, android.os.Looper.getMainLooper())
            }
            val cached = runCatching {
                lm?.getLastKnownLocation(provider)
                    ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }.getOrNull()
            if (cached != null) updateLoc(cached)
        } else {
            Toast.makeText(context, "请开启 GPS / 网络定位服务", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    overScrollMode = android.view.View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    setBackgroundColor(0)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = false

                    webViewClient = WebViewClient()
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                            msg?.let {
                                android.util.Log.i("RilocWebView", "[${it.messageLevel()}] ${it.message()}")
                            }
                            return true
                        }
                    }
                    addJavascriptInterface(webAppInterface, "Android")
                    loadUrl("file:///android_asset/map.html")
                    webViewRef = this
                }

            },
            modifier = Modifier.fillMaxSize(),
        )


        // ── Top Header: AutoNavi Style Floating Search Capsule Card ──
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = colorScheme.surface.copy(alpha = 0.95f)
            ),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).height(48.dp),
                        placeholder = { Text(if (isSearching) "搜索中…" else "搜索地点、公交、路线…", fontSize = 13.sp, color = colorScheme.onSurfaceVariantSummary) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                        trailingIcon = {
                            IconButton(onClick = { performSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索", tint = colorScheme.primary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surfaceContainer,
                            unfocusedContainerColor = colorScheme.surfaceContainer,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { locateRealDevice() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "真实位置",
                            tint = colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "历史与收藏",
                            tint = colorScheme.primary
                        )
                    }
                }

                // Live Search Autocomplete Dropdown List
                if (searchSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        colors = CardDefaults.defaultColors(color = colorScheme.surfaceContainer),
                    ) {
                        Column {
                            searchSuggestions.take(5).forEach { res ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = res.name
                                            searchSuggestions = emptyList()
                                            keyboardController?.hide()
                                            LocationHub.update(res.lat, res.lon)
                                            HistoryManager.add(res.name, res.lat, res.lon)
                                            webViewRef?.evaluateJavascript("jsSetStart(${res.lat}, ${res.lon}, '${res.name}');", null)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(res.name, fontSize = 13.sp, color = colorScheme.onSurface)
                                        Text(res.address, fontSize = 11.sp, color = colorScheme.onSurfaceVariantSummary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Address & Coordinate Detail Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = addressName,
                            fontSize = 13.sp,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPlaying) "已开启虚拟定位" else "虚拟定位已停止",
                                fontSize = 11.sp,
                                color = if (isPlaying) Color(0xFF36D167) else colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "%.5f, %.5f".format(current.lat, current.lon),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    MiuixChip("收藏", selected = false) {
                        BookmarkManager.save(addressName.take(12), current.lat, current.lon)
                        Toast.makeText(context, "已收藏当前位置", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ── Right-Side Floating Map Tool Column ──
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MapActionButton(icon = Icons.Default.Add, contentDescription = "放大") {
                webViewRef?.evaluateJavascript("jsZoomIn();", null)
            }
            MapActionButton(icon = Icons.Default.Remove, contentDescription = "缩小") {
                webViewRef?.evaluateJavascript("jsZoomOut();", null)
            }
            MapActionButton(icon = Icons.Default.MyLocation, contentDescription = "回到中心") {
                webViewRef?.evaluateJavascript("jsSetStart(${current.lat}, ${current.lon}, '当前位置');", null)
            }
            MapActionButton(icon = Icons.Default.Layers, contentDescription = "图层选择") {
                showStyleSelector = !showStyleSelector
            }
        }

        // ── Bottom Control Panel ──
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = bottomInnerPadding + 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = colorScheme.surface.copy(alpha = 0.95f)
            ),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Segmented Capsule Control for MapMode
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(end = 12.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainerHigh),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MapMode.entries.forEach { mode ->
                            val selected = (currentMode == mode)
                            val label = when (mode) {
                                MapMode.STATIC -> "静态"
                                MapMode.JOYSTICK -> "摇杆"
                                MapMode.ROUTE -> "漫游"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        currentMode = mode
                                        when (mode) {
                                            MapMode.STATIC -> {
                                                engine.stop()
                                                routeEngine.stop()
                                            }
                                            MapMode.JOYSTICK -> {
                                                routeEngine.stop()
                                            }
                                            MapMode.ROUTE -> {
                                                engine.stop()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else colorScheme.onSurfaceVariantSummary,
                                    ),
                                )
                            }
                        }
                    }

                    FloatingActionButton(

                        onClick = {
                            isPlaying = !isPlaying
                            Prefs.setPlaying(isPlaying)
                            if (!isPlaying) {
                                engine.stop()
                                routeEngine.stop()
                            } else {
                                when (currentMode) {
                                    MapMode.JOYSTICK -> engine.startJoystick(joystickSpeed)
                                    MapMode.ROUTE -> routeEngine.start(speed = joystickSpeed)
                                    MapMode.STATIC -> {}
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        containerColor = if (isPlaying) Color(0xFFE53935) else Color(0xFF1E88E5),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "停止" else "开始",
                            tint = Color.White,
                        )
                    }
                }

                // Controls for Joystick mode
                if (currentMode == MapMode.JOYSTICK || currentMode == MapMode.ROUTE) {
                    Column(Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("速度", fontSize = 13.sp, color = colorScheme.onSurface)
                            Slider(
                                value = joystickSpeed,
                                onValueChange = {
                                    joystickSpeed = it
                                    Prefs.setUiFloat("joystick_speed", it)
                                    if (engine.mode == MoveEngine.Mode.JOYSTICK) {
                                        engine.stop()
                                        engine.startJoystick(it)
                                    }
                                },
                                valueRange = 0.5f..30f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            )
                            Text(
                                "%.1f m/s".format(joystickSpeed),
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariantSummary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiuixChip("步行", selected = kotlin.math.abs(joystickSpeed - 1.5f) < 0.2f) {
                                joystickSpeed = 1.5f
                                Prefs.setUiFloat("joystick_speed", 1.5f)
                            }
                            MiuixChip("骑行", selected = kotlin.math.abs(joystickSpeed - 4.5f) < 0.2f) {
                                joystickSpeed = 4.5f
                                Prefs.setUiFloat("joystick_speed", 4.5f)
                            }
                            MiuixChip("驾车", selected = kotlin.math.abs(joystickSpeed - 15.0f) < 0.2f) {
                                joystickSpeed = 15.0f
                                Prefs.setUiFloat("joystick_speed", 15.0f)
                            }
                            MiuixChip("高速", selected = kotlin.math.abs(joystickSpeed - 30.0f) < 0.2f) {
                                joystickSpeed = 30.0f
                                Prefs.setUiFloat("joystick_speed", 30.0f)
                            }
                        }
                    }
                }

                // Controls for Route mode
                if (currentMode == MapMode.ROUTE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "路线节点: ${routeWaypoints.size} 个 (按住地图划动轨迹)",
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )
                        MiuixChip("清空路线", selected = false) {
                            routeWaypoints.clear()
                            routeEngine.clearRoute()
                            webViewRef?.evaluateJavascript("jsClearRoute();", null)
                        }
                    }
                }

                // Expandable Map Style Selector Row
                if (showStyleSelector) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "地图图层",
                            fontSize = 13.sp,
                            color = colorScheme.onSurface,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MapStyle.entries.forEach { style ->
                                MiuixChip(
                                    text = style.label,
                                    selected = mapStyle == style,
                                ) {
                                    mapStyle = style
                                    MapStyles.setCurrent(style)
                                    webViewRef?.evaluateJavascript("jsSetMapStyle('${style.key}');", null)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Joystick Widget overlay (bottom-right) ──
        if (currentMode == MapMode.JOYSTICK) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = bottomInnerPadding + 160.dp)
            ) {
                Joystick(onVectorChange = { x, y -> engine.setJoystickVector(x, y) })
            }
        }

        // ── History & Bookmarks Quick Selector Dialog ──
        if (showHistorySheet) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(0.92f)
                    .height(400.dp),
                colors = CardDefaults.defaultColors(color = colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("收藏与历史记录", fontSize = 16.sp, color = colorScheme.primary)
                        MiuixChip("关闭", selected = false) { showHistorySheet = false }
                    }

                    Spacer(Modifier.height(12.dp))

                    val bookmarks = remember { BookmarkManager.getAll() }
                    val history = remember { HistoryManager.getAll() }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bookmarks.isNotEmpty()) {
                            item { Text("收藏书签", fontSize = 13.sp, color = colorScheme.primary) }
                            items(bookmarks) { b ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.surfaceContainer)
                                        .clickable {
                                            LocationHub.update(b.lat, b.lon)
                                            webViewRef?.evaluateJavascript("jsSetStart(${b.lat}, ${b.lon}, '${b.name}');", null)
                                            showHistorySheet = false
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(b.name, fontSize = 14.sp, color = colorScheme.onSurface)
                                    Text("%.4f, %.4f".format(b.lat, b.lon), fontSize = 12.sp, color = colorScheme.onSurfaceVariantSummary)
                                }
                            }
                        }

                        if (history.isNotEmpty()) {
                            item { Text("历史搜索与选点", fontSize = 13.sp, color = colorScheme.primary, modifier = Modifier.padding(top = 10.dp)) }
                            items(history) { h ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.surfaceContainer)
                                        .clickable {
                                            LocationHub.update(h.lat, h.lon)
                                            webViewRef?.evaluateJavascript("jsSetStart(${h.lat}, ${h.lon}, '${h.name}');", null)
                                            showHistorySheet = false
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(h.name, fontSize = 14.sp, color = colorScheme.onSurface)
                                    Text("%.4f, %.4f".format(h.lat, h.lon), fontSize = 12.sp, color = colorScheme.onSurfaceVariantSummary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(colorScheme.surface.copy(alpha = 0.95f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MiuixChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) colorScheme.primary else colorScheme.surfaceContainer
    val fg = if (selected) Color.White else colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = fg,
        )
    }
}
