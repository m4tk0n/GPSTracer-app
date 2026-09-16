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

        // Vychozi limit disk cache je jen 600 MB a stare dlazdice se automaticky
        // mazou (trimuji), kdyz se prekroci - to by mohlo smazat i schvalne
        // stazene offline oblasti. Zvysujeme limit, ať offline mapy zbytecne nemizi.
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 1_500_000_000L // 1.5 GB
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 1_300_000_000L // 1.3 GB

        SyncWorker.schedulePeriodic(this)
    }
}
