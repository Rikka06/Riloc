# Riloc

基于 **Rikka06/Android-UI-Template** UI 框架的全功能虚拟定位 Xposed 模块（libxposed，LSPosed 1.9+）。

## 功能

- **全功能虚拟定位** — 拦截 GPS / Network / Fused Provider 全类型定位数据
- **反检测** — `isMock()` / `isFromMockProvider()` → false、Extras 剥离、AppOps 隐藏、Settings.Secure 隐藏、Provider 归一化、mFieldsMask 清除
- **摇杆实时移动** — 屏幕拖动摇杆控制运动方向，速度可调
- **多地图样式** — 高德地图 / 高德卫星 / CartoDB / OpenStreetMap 一键切换
- **坐标随机化** — 在目标位置周围随机偏移，模拟真实 GPS 抖动
- **精度/海拔/速度伪造** — 可单独控制各参数的启用和数值
- **Geocoder 一致性** — 反向地理编码返回模拟坐标
- **Wi-Fi / 蜂窝指纹隐藏** — 清空 WLAN 扫描结果和基站信息
- **深度系统 Hook**（可选）— system_server 级 GPS 分发拦截 / GNSS 屏蔽 / MIUI 模糊定位绕过

## UI 架构（保留模板整体）

UI 完整保留 Android-UI-Template 的框架与设计系统，只做了以下"添加功能 + 按钮改名 + 位置微调"：

| 模板原始 | Riloc 现在 | 改动 |
|---------|-----------|------|
| 底部栏/侧边栏 4 个页签（首页/超级用户/模块/设置） | 地图 / 应用 / 设置（3 个页签） | 改按钮名字与图标，移除轨迹页 |
| 首页 (HomePager) | 地图页：高德等多瓦片源地图 + 虚拟定位/摇杆控制 | 替换内容 |
| 模块 (ModulePager) | 目标应用页：安装应用选择 | 替换内容 |
| 设置页 (SettingPager) | 设置页顶部追加"定位参数 / 反检测 / 系统级 Hook"设置卡 | 添加功能（位置微调） |
| 关于页 | 关于页文案改为 Riloc | 改文案 |
| 主题/模糊/悬浮栏/UI 模式等设置 | 全部保留 | 无改动 |
| MainActivity/Navigation3/侧边栏/双主题组件体系 | 全部保留 | 无改动 |

### 目录结构

```
app/src/main/java/com/riloc/app/
├── KernelSUApplication.kt    — 模板 Application + LSPosed 服务绑定（融合）
├── Natives.kt / Kernels.kt   — 模板 mock 原生桥（保留，无 root 依赖）
├── data/ profile/            — 模板数据层（保留）
├── ui/                       — 模板 UI 框架（MainActivity/Navigation3/主题/组件）
│   ├── screen/riloc/         — 新增：RilocMapPager / RilocAppsPager / RilocSettingsContent* / MapStyles
│   └── screen/settings/      — 模板设置页（顶部嵌入 Riloc 设置卡）
├── common/                   — Constants / Prefs（remote SharedPreferences IPC）
├── engine/                   — GeoMath / LocationHub / MoveEngine（摇杆移动）
└── xposed/                   — Xposed 模块入口 + 7 个 Hook 安装器 + LocationState
```

### Hook 安装器

| 安装器 | 目标进程 | 职责 |
|--------|---------|------|
| `LocationApiHooks` | 目标应用 | Location getLatitude/getLongitude/getAccuracy/… |
| `LocationManagerHooks` | 目标应用 | getLastKnownLocation / getCurrentLocation / isProviderEnabled |
| `MockHideHooks` | 目标应用 | isMock/isFromMockProvider、extras、Settings.Secure、AppOps、Provider 归一化 |
| `GeocoderHooks` | 目标应用 | isPresent、getFromLocation |
| `WifiHooks` | 目标应用 | getScanResults、getConnectionInfo |
| `TelephonyHooks` | 目标应用 / com.android.phone | getAllCellInfo、getCellLocation、getNetworkOperator |
| `SystemHooks` | system_server | LocationManagerService、LocationProviderManager、GNSS 注册、MIUI 模糊定位 |

### IPC 协议

Manager 应用通过 **LSPosed Remote SharedPreferences**（组名 `"settings"`）与 Xposed 模块通信：应用侧 `XposedServiceHelper` 绑定 LSPosed 服务后写入，模块侧 `getRemotePreferences("settings")` 读取。

## 安装与使用

### 前提

- 已安装 **LSPosed**（1.9+），Android 8+（minSdk 26）

### 步骤

1. 安装 APK（`app/build/outputs/apk/debug/app-debug.apk`）
2. LSPosed → 模块 → 启用 Riloc，作用域勾选目标应用（可选加 `system` / `com.android.phone` 启用深度 Hook）
3. 打开 Riloc → 地图页点击选择位置 → ▶ 开始（静态/摇杆/漫游）
4. 轨迹页长按地图画路线 → 选速度档位 → 开始模拟
5. 设置页顶部配置定位参数与反检测开关

## 构建

```bash
./gradlew :app:assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 参考项目

- [Android-UI-Template](https://github.com/Rikka06/Android-UI-Template) — UI 框架（整体保留）
- [XposedFakeLocation](https://github.com/noobexon1/XposedFakeLocation) / [HideMockLocation](https://github.com/auag0/HideMockLocation) / [xPoint](https://github.com/hazbu/xPoint) / [FuckLocation](https://github.com/Mikotwa/FuckLocation) — 定位与反检测实现参考

## License

仅供学习与研究使用。
