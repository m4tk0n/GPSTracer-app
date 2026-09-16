package com.example.cycletracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cycletracker.R
import com.example.cycletracker.db.AppDatabase
import com.example.cycletracker.db.OfflineRegionEntity
import com.example.cycletracker.map.MapTileSources
import com.example.cycletracker.map.OfflineMapManager
import com.example.cycletracker.map.OfflineRegion
import com.example.cycletracker.map.OfflineRegions
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.*

class OfflineMapsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_maps)

        db = AppDatabase.getInstance(this)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(MapTileSources.STREETS)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(6.5)
        mapView.controller.setCenter(GeoPoint(49.80, 15.47)) // stred CR

        val regionListContainer = findViewById<android.widget.LinearLayout>(R.id.regionList)

        db.offlineRegionDao().getAllLive().observe(this) { downloaded ->
            val downloadedByName = downloaded.associateBy { it.name }
            regionListContainer.removeAllViews()
            OfflineRegions.ALL.forEach { region ->
                val row = LayoutInflater.from(this).inflate(R.layout.item_offline_region, regionListContainer, false)
                bindRow(row, region, downloadedByName[region.name])
                regionListContainer.addView(row)
            }
        }
    }

    private fun bindRow(row: View, region: OfflineRegion, existing: OfflineRegionEntity?) {
        val tvName = row.findViewById<TextView>(R.id.tvRegionName)
        val tvStatus = row.findViewById<TextView>(R.id.tvRegionStatus)
        val progressBar = row.findViewById<ProgressBar>(R.id.progressBar)
        val btnAction = row.findViewById<Button>(R.id.btnAction)

        tvName.text = region.name

        fun showDownloaded(entity: OfflineRegionEntity) {
            tvStatus.text = "Staženo ${dateFormat.format(Date(entity.downloadedAt))} · ${entity.tileCount} dlaždic"
            btnAction.text = "Smazat"
            progressBar.visibility = View.GONE
        }

        fun showNotDownloaded() {
            tvStatus.text = "Nestaženo"
            btnAction.text = "Stáhnout"
            progressBar.visibility = View.GONE
        }

        if (existing != null) showDownloaded(existing) else showNotDownloaded()

        btnAction.setOnClickListener {
            if (existing != null) {
                // Smazat
                btnAction.isEnabled = false
                OfflineMapManager.delete(
                    this, mapView, region,
                    OfflineRegions.DEFAULT_ZOOM_MIN, OfflineRegions.DEFAULT_ZOOM_MAX
                )
                lifecycleScope.launch {
                    db.offlineRegionDao().delete(existing)
                    btnAction.isEnabled = true
                }
            } else {
                // Stahnout
                btnAction.isEnabled = false
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                tvStatus.text = "Připravuji stahování..."

                OfflineMapManager.download(
                    this, mapView, region,
                    OfflineRegions.DEFAULT_ZOOM_MIN, OfflineRegions.DEFAULT_ZOOM_MAX,
                    object : OfflineMapManager.ProgressListener {
                        private var total = 0

                        override fun onStart(totalTiles: Int) {
                            total = totalTiles
                            tvStatus.text = "Stahuji 0 / $totalTiles dlaždic..."
                        }

                        override fun onProgress(done: Int, currentZoom: Int, zoomMin: Int, zoomMax: Int) {
                            if (total > 0) {
                                progressBar.progress = (done * 100 / total).coerceIn(0, 100)
                            }
                            tvStatus.text = "Stahuji $done / $total dlaždic (zoom $currentZoom)..."
                        }

                        override fun onComplete(totalTiles: Int) {
                            lifecycleScope.launch {
                                db.offlineRegionDao().insert(
                                    OfflineRegionEntity(
                                        name = region.name,
                                        minLat = region.south,
                                        maxLat = region.north,
                                        minLon = region.west,
                                        maxLon = region.east,
                                        zoomMin = OfflineRegions.DEFAULT_ZOOM_MIN,
                                        zoomMax = OfflineRegions.DEFAULT_ZOOM_MAX,
                                        downloadedAt = System.currentTimeMillis(),
                                        tileCount = total
                                    )
                                )
                                btnAction.isEnabled = true
                            }
                        }

                        override fun onError(errors: Int) {
                            tvStatus.text = "Chyba při stahování ($errors) - zkus to znovu, zkontroluj připojení"
                            progressBar.visibility = View.GONE
                            btnAction.isEnabled = true
                            btnAction.text = "Stáhnout"
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
