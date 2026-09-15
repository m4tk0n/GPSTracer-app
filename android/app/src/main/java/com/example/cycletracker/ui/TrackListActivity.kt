package com.example.cycletracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cycletracker.R
import com.example.cycletracker.db.AppDatabase
import com.example.cycletracker.db.TrackEntity
import java.text.SimpleDateFormat
import java.util.*

class TrackListActivity : AppCompatActivity() {

    private lateinit var adapter: TrackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TrackAdapter { track ->
            startActivity(Intent(this, TrackDetailActivity::class.java).putExtra("trackId", track.id))
        }
        recyclerView.adapter = adapter

        AppDatabase.getInstance(this).trackDao().getAllTracksLive().observe(this) { tracks ->
            adapter.submitList(tracks)
        }
    }
}

private class TrackAdapter(val onClick: (TrackEntity) -> Unit) :
    RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    private var items: List<TrackEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault())

    fun submitList(list: List<TrackEntity>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSummary: TextView = view.findViewById(R.id.tvSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = items[position]
        holder.tvDate.text = dateFormat.format(Date(track.startedAt))
        val km = track.distanceMeters / 1000.0
        holder.tvSummary.text = "%.2f km · %.0f m prevyseni · %.1f km/h prumer · %s"
            .format(km, track.elevationGainMeters, track.avgSpeedKmh, stateLabel(track.state))
        holder.itemView.setOnClickListener { onClick(track) }
    }

    private fun stateLabel(state: String) = when (state) {
        TrackEntity.STATE_RUNNING -> "probiha"
        TrackEntity.STATE_PAUSED -> "pauza"
        else -> "hotovo"
    }

    override fun getItemCount() = items.size
}
