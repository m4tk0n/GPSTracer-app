package com.example.cycletracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cycletracker.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<android.widget.Button>(R.id.btnTracks).setOnClickListener {
            startActivity(Intent(this, TrackListActivity::class.java))
        }
        findViewById<android.widget.Button>(R.id.btnOfflineMaps).setOnClickListener {
            startActivity(Intent(this, OfflineMapsActivity::class.java))
        }
    }
}
