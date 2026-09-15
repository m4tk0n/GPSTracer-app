package com.example.cycletracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Appka neni na Google Play, takze nema automaticke aktualizace. Misto toho
 * si pri startu sama stahne maly version.json ze serveru (verejny, bez
 * prihlaseni) a porovna s cislem verze, kterou ma prave nainstalovanou.
 *
 * Kdyz vydas novou verzi appky:
 *  1) postav novy APK (GitHub Actions)
 *  2) nahraj ho na server jako downloads/tracer.apk (prepsat)
 *  3) uprav downloads/version.json - zvys "versionCode" o 1
 * Diky pevnemu debug klici (viz debug.keystore) se novy APK da nainstalovat
 * "pres" ten stary, zadne odinstalovani neni potreba.
 */
object UpdateChecker {
    private const val VERSION_URL = "https://7x.wz.cz/tracer/downloads/version.json"
    private val client = OkHttpClient()

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String?
    )

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    apkUrl = json.getString("apkUrl"),
                    notes = json.optString("notes").ifBlank { null }
                )
            }
        } catch (e: Exception) {
            null // offline nebo server nedostupny - proste nic neupozornit
        }
    }

    /** Otevre APK odkaz v prohlizeci - spusti se stazeni a system pak nabidne instalaci. */
    fun openDownload(context: Context, apkUrl: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
    }
}
