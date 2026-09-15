package com.example.cycletracker.map

/** Vzdalenost mezi dvema GPS body v metrech (Haversinuv vzorec). */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

/** Prevede seznam bodu (lat, lng, nadmorska vyska) na (kumulativni km, vyska) pro vyskovy profil. */
fun buildElevationProfile(points: List<Triple<Double, Double, Double>>): List<Pair<Float, Float>> {
    if (points.isEmpty()) return emptyList()
    var cumulativeM = 0.0
    val result = ArrayList<Pair<Float, Float>>(points.size)
    result.add(0f to points[0].third.toFloat())
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        cumulativeM += haversineMeters(prev.first, prev.second, cur.first, cur.second)
        result.add((cumulativeM / 1000.0).toFloat() to cur.third.toFloat())
    }
    return result
}
