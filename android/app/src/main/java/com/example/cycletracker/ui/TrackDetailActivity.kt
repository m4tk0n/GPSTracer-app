package com.example.cycletracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cycletracker.R
import com.example.cycletracker.db.AppDatabase
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class TrackDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_detail)

        val trackId = intent.getLongExtra("trackId", -1)
        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@TrackDetailActivity).trackDao()
            val track = dao.getTrack(trackId) ?: return@launch
            val points = dao.getPointsForTrack(trackId)

            val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
            if (geoPoints.isNotEmpty()) {
                val line = Polyline(mapView)
                line.setPoints(geoPoints)
                mapView.overlays.add(line)
                mapView.controller.setZoom(14.0)
                mapView.controller.setCenter(geoPoints[geoPoints.size / 2])
            }
            mapView.invalidate()

            findViewById<android.widget.TextView>(R.id.tvStats).text =
                "Vzdalenost: %.2f km\nPrevyseni: %.0f m\nPrumerna rychlost: %.1f km/h\nDoba jizdy: %s"
                    .format(
                        track.distanceMeters / 1000.0,
                        track.elevationGainMeters,
                        track.avgSpeedKmh,
                        formatDuration(track.movingTimeMillis)
                    )
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "%dh %02dm".format(h, m)
    }
}
