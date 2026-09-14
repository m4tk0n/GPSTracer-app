package com.example.cycletracker

import android.app.Application
import android.preference.PreferenceManager
import com.example.cycletracker.network.RetrofitClient
import com.example.cycletracker.sync.SyncWorker
import org.osmdroid.config.Configuration

class CycleTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)

        // osmdroid potrebuje user agent + cache nastaveni, jinak OSM tiles blokuje
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        SyncWorker.schedulePeriodic(this)
    }
}
