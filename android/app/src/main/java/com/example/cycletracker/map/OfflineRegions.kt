package com.example.cycletracker.map

/** Jedna nabizena oblast ke stazeni - jmeno + ohranicujici obdelnik (sever/jih/vychod/zapad). */
data class OfflineRegion(
    val name: String,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)

/**
 * Predpripravene oblasti. Souradnice jsou zaokrouhlene obdelniky (ne presne
 * hranice kraju) - staci na ucel "stahni si mapu tam, kam jedes".
 * Snadno se da pridat dalsi zem/oblast pridanim dalsi polozky do seznamu.
 */
object OfflineRegions {

    // Doporuceny rozsah zoomu pro stazenou oblast - dost na navigaci i detail ulic,
    // bez zbytecne velkeho objemu dat. Da se v UI pripadne nabidnout na vyber.
    const val DEFAULT_ZOOM_MIN = 8
    const val DEFAULT_ZOOM_MAX = 15

    val CZECH_REGIONS = listOf(
        OfflineRegion("Praha a okolí", north = 50.20, south = 49.90, east = 14.75, west = 14.20),
        OfflineRegion("Jihomoravský kraj", north = 49.45, south = 48.60, east = 17.15, west = 15.90),
        OfflineRegion("Jihočeský kraj", north = 49.55, south = 48.55, east = 15.30, west = 13.40),
        OfflineRegion("Plzeňský kraj", north = 50.10, south = 49.10, east = 13.90, west = 12.40),
        OfflineRegion("Karlovarský kraj", north = 50.55, south = 49.90, east = 13.30, west = 12.10),
        OfflineRegion("Ústecký kraj", north = 50.90, south = 50.20, east = 14.35, west = 13.05),
        OfflineRegion("Liberecký kraj", north = 50.90, south = 50.45, east = 15.35, west = 14.35),
        OfflineRegion("Královéhradecký kraj", north = 50.65, south = 50.05, east = 16.50, west = 15.35),
        OfflineRegion("Pardubický kraj", north = 50.20, south = 49.55, east = 16.90, west = 15.55),
        OfflineRegion("Kraj Vysočina", north = 49.75, south = 49.05, east = 16.10, west = 14.90),
        OfflineRegion("Olomoucký kraj", north = 50.30, south = 49.35, east = 17.60, west = 16.55),
        OfflineRegion("Moravskoslezský kraj", north = 50.15, south = 49.45, east = 18.85, west = 17.45),
        OfflineRegion("Zlínský kraj", north = 49.55, south = 48.95, east = 18.15, west = 17.15),
        OfflineRegion("Středočeský kraj", north = 50.45, south = 49.35, east = 15.80, west = 13.70)
    )

    val OTHER_COUNTRIES = listOf(
        OfflineRegion("Slovensko (celé)", north = 49.65, south = 47.70, east = 22.60, west = 16.80),
        OfflineRegion("Rakousko (celé)", north = 49.05, south = 46.35, east = 17.20, west = 9.50),
        OfflineRegion("Polsko - jih", north = 51.20, south = 49.00, east = 22.90, west = 14.90),
        OfflineRegion("Německo - Sasko/Bavorsko", north = 51.70, south = 47.25, east = 15.05, west = 8.95)
    )

    val ALL = CZECH_REGIONS + OTHER_COUNTRIES
}
