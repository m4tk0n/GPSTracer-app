package com.example.cycletracker.map

import android.content.Context
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

object OfflineMapManager {

    interface ProgressListener {
        fun onStart(totalTiles: Int)
        fun onProgress(done: Int, currentZoom: Int, zoomMin: Int, zoomMax: Int)
        fun onComplete(totalTiles: Int)
        fun onError(errors: Int)
    }

    private fun boundingBox(region: OfflineRegion) =
        BoundingBox(region.north, region.east, region.south, region.west)

    /**
     * Stahne dlazdice pro danou oblast do standardni osmdroid disk cache.
     * mapView musi mit pred volanim nastaveny pozadovany zdroj mapy
     * (setTileSource) - CacheManager stahuje presne to, co mapView prave pouziva.
     */
    fun download(
        context: Context,
        mapView: MapView,
        region: OfflineRegion,
        zoomMin: Int,
        zoomMax: Int,
        listener: ProgressListener
    ) {
        val cacheManager = CacheManager(mapView)
        cacheManager.downloadAreaAsyncNoUI(
            context, boundingBox(region), zoomMin, zoomMax,
            object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    listener.onComplete(0)
                }

                override fun onTaskFailed(errors: Int) {
                    listener.onError(errors)
                }

                override fun downloadStarted() {
                    // no-op, updateProgress/setPossibleTilesInArea nam staci
                }

                override fun setPossibleTilesInArea(total: Int) {
                    listener.onStart(total)
                }

                override fun updateProgress(progress: Int, currentZoomLevel: Int, pZoomMin: Int, pZoomMax: Int) {
                    listener.onProgress(progress, currentZoomLevel, pZoomMin, pZoomMax)
                }
            }
        )
    }

    /** Smaze dlazdice stazene pro danou oblast (jen z aktualniho zdroje mapView). */
    fun delete(context: Context, mapView: MapView, region: OfflineRegion, zoomMin: Int, zoomMax: Int) {
        val cacheManager = CacheManager(mapView)
        cacheManager.cleanAreaAsync(context, boundingBox(region), zoomMin, zoomMax)
    }
}
