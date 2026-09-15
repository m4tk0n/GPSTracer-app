package com.example.cycletracker.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/**
 * Sada mapovych podkladu.
 *
 * STREETS pouziva primo standardni OpenStreetMap dlazdice (TileSourceFactory.MAPNIK).
 * Puvodne appka bezela pod nazvem "com.example.cycletracker" - stejny nazev
 * pouziva tisice tutorialovych appek, a OSM servery takove hromadne
 * zneuzivany User-Agent/balicek casto plosne blokuji (viz jejich tile usage
 * policy: https://operations.osmfoundation.org/policies/tiles/). Po zmene
 * na unikatni "cz.tracer.gpsapp" by uz OSM dlazdice mely fungovat normalne.
 *
 * TOPO a SATELLITE jsou nezavisle zdroje (nepotrebuji API klic) pro
 * prepinani reliefu.
 */
object MapTileSources {

    val STREETS: OnlineTileSourceBase = TileSourceFactory.MAPNIK

    val TOPO: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "OpenTopoMap", 0, 17, 256, ".png",
        arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${baseUrl}$z/$x/$y.png"
        }
    }

    val SATELLITE: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "EsriWorldImagery", 0, 19, 256, ".jpg",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            // Esri ma poradi z/y/x (ne z/x/y jako vetsina ostatnich)
            return "${baseUrl}$z/$y/$x"
        }
    }

    val ALL = listOf(STREETS, TOPO, SATELLITE)
    val LABELS = listOf("Ulice", "Turistická", "Satelitní")
}
