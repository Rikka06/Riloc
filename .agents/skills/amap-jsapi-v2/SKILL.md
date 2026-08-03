---
name: amap-jsapi-v2
description: Official AutoNavi AMap JS API v2.0 integration guide, security configuration rules, Map lifecycle, AutoComplete plugin, vector layers, marker overlays, driving route planning, and JavascriptInterface bridge patterns.
---

# AutoNavi AMap JS API v2.0 Official Development Guide & Skill

This skill provides comprehensive instructions for integrating and using the **official AutoNavi AMap JS API v2.0** in web applications and Android WebView containers.

---

## 1. Security Configuration (`window._AMapSecurityConfig`)

> [!IMPORTANT]
> `window._AMapSecurityConfig` **MUST** be initialized BEFORE loading the AMap script tag.

```javascript
window._AMapSecurityConfig = {
    securityJsCode: 'YOUR_AMAP_SECURITY_SECRET', // 必填：高德安全密钥
};
```

---

## 2. Script Loader (`webapi.amap.com/maps`)

Load the official AMap JS API v2.0 script with required plugins:

```html
<script src="https://webapi.amap.com/maps?v=2.0&key=YOUR_AMAP_WEB_KEY&plugin=AMap.AutoComplete,AMap.PlaceSearch,AMap.DrivingPolyline"></script>
```

---

## 3. Map Initialization & Controls

### Basic Map Instance
```javascript
var map = new AMap.Map('container', {
    zoom: 16,                               // 初始缩放级别 (3~20)
    center: [116.4074, 39.9042],            // 初始中心坐标 [经度, 纬度]
    resizeEnable: true,                     // 调整大小时自动适应
    rotateEnable: true,                     // 开启旋转手势
    pitchEnable: true,                      // 开启 3D 视角倾斜
    viewMode: '3D',                         // '2D' 或 '3D' 矢量模式
});
```

### Layer Switching
```javascript
// 切换普通矢量图层
map.setLayers([new AMap.TileLayer()]);

// 切换卫星图层
map.setLayers([new AMap.TileLayer.Satellite()]);

// 开启实时交通图层
var traffic = new AMap.TileLayer.Traffic();
map.add(traffic);
```

---

## 4. Place Search Autocomplete (`AMap.AutoComplete`)

```javascript
var autoSuggest = new AMap.AutoComplete({ city: '全国' });

function searchKeywords(keyword, callback) {
    autoSuggest.search(keyword, function(status, result) {
        if (status === 'complete' && result.tips) {
            callback(result.tips);
        }
    });
}
```

---

## 5. Marker & Vector Overlay Operations

### Adding Custom SVG Markers
```javascript
var iconSvg = '<div style="width:30px;height:40px;"><svg viewBox="0 0 24 24" width="30" height="40" xmlns="http://www.w3.org/2000/svg"><path d="M12 0C7.58 0 4 3.58 4 8c0 5.25 7 13 8 16 1-3 8-10.75 8-16 0-4.42-3.58-8-8-8z" fill="#1E88E5"/><circle cx="12" cy="8" r="3" fill="#FFFFFF"/></svg></div>';

var marker = new AMap.Marker({
    position: [116.4074, 39.9042],
    content: iconSvg,
    offset: new AMap.Pixel(-15, -40),
});

map.add(marker);
```

### Polyline Route Drawing
```javascript
var polyline = new AMap.Polyline({
    path: [
        [116.4074, 39.9042],
        [116.4174, 39.9142]
    ],
    strokeColor: "#1E88E5",
    strokeWeight: 6,
    strokeOpacity: 0.9,
    lineJoin: 'round'
});

map.add(polyline);
```

---

## 6. Android WebView JavascriptInterface Integration

### HTML/JS Side Bridge (`map.html`)
```javascript
window.onload = function() {
    var key = '8325164e247e15eea68b59e89200988b';
    var secret = '';
    if (window.Android) {
        try {
            var config = window.Android.getAMapConfig();
            var parts = config.split('|');
            if (parts[0]) key = parts[0];
            if (parts[1]) secret = parts[1];
        } catch(e) {}
    }
    startLoadMap(key, secret);
};
```

### Kotlin Side Bridge (`WebAppInterface.kt`)
```kotlin
class WebAppInterface {
    @JavascriptInterface
    fun getAMapConfig(): String = "AMAP_KEY|AMAP_SECRET"

    @JavascriptInterface
    fun onPointSelected(lat: Double, lng: Double, name: String) {
        // Handle coordinate selection
    }
}
```

---

## 7. Best Practices & Troubleshooting

1. **Security Mismatch Error (`INVALID_USER_KEY`)**: Always verify `window._AMapSecurityConfig.securityJsCode` matches the Web Service key's security secret from the AMap developer console.
2. **WebView File Access**: Always enable `allowFileAccessFromFileURLs` and `allowUniversalAccessFromFileURLs` in Android WebView settings.
3. **No External CDNs**: Avoid non-Chinese CDNs (like unpkg) in Android WebView assets; load script directly from `https://webapi.amap.com`.
