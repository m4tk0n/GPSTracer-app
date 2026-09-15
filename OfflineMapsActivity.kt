package com.example.cycletracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cycletracker.R

/**
 * Obrazovka pro správu offline map.
 * Samotné stahování oblastí bude doplněno v další části.
 */
class OfflineMapsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_maps)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Offline mapy"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
