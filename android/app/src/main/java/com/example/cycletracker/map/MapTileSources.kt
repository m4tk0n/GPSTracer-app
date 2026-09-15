package com.example.cycletracker.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * Vlastni sada mapovych podkladu. Nepouzivame primo TileSourceFactory.MAPNIK,
 * protoze OSM Foundation blokuje adresy prohresujici se proti jejich "tile
 * usage policy" (https://operations.osmfoundation.org/policies/tiles/) - a
 * casto pod to spadne cely balik "com.example.*", protoze pod timhle nazvem
 * bezi tisice tutorialovych appek co pravidla neresi. Tyhle 3 zdroje jsou
 * zdarma bez API klice a maji volnejsi/jine limity.
 */
object MapTileSources {

    val STREETS: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "CartoVoyager", 0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${baseUrl}$z/$x/$y.png"
        }
    }

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
