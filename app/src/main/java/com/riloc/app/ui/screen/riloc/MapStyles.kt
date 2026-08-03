package com.riloc.app.ui.screen.riloc

import com.riloc.app.common.Prefs
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/** Supported map tile styles aligned with GlobalTraveling HD tile engines. */
enum class MapStyle(val key: String, val label: String) {
    AMAP("amap", "高德地图"),
    AMAP_SAT("amap_sat", "高德卫星"),
    CARTO("carto", "CartoDB"),
    OSM("osm", "OSM");

    companion object {
        fun fromKey(key: String?): MapStyle = entries.firstOrNull { it.key == key } ?: AMAP
    }
}

object MapStyles {
    fun current(): MapStyle = MapStyle.fromKey(Prefs.uiString("map_style", null))

    fun setCurrent(style: MapStyle) = Prefs.setUiString("map_style", style.key)

    fun createTileSource(style: MapStyle): ITileSource = when (style) {
        MapStyle.AMAP -> object : OnlineTileSourceBase(
            "AMapVectorHD", 0, 19, 256, ".png",
            arrayOf("https://webrd01.is.autonavi.com/appmaptile")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "https://webrd0${(x % 4) + 1}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$zoom"
            }
        }
        MapStyle.AMAP_SAT -> object : OnlineTileSourceBase(
            "AMapSatHD", 0, 19, 256, ".png",
            arrayOf("https://webst01.is.autonavi.com/appmaptile")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "https://webst0${(x % 4) + 1}.is.autonavi.com/appmaptile?style=6&x=$x&y=$y&z=$zoom"
            }
        }
        MapStyle.CARTO -> object : OnlineTileSourceBase(
            "CartoVoyager", 0, 19, 256, ".png",
            arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                val server = arrayOf("a", "b", "c", "d")[(x + y) % 4]
                return "https://$server.basemaps.cartocdn.com/rastertiles/voyager/$zoom/$x/$y.png"
            }
        }
        MapStyle.OSM -> TileSourceFactory.MAPNIK
    }
}
