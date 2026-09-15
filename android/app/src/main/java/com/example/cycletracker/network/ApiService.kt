package com.example.cycletracker.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val userId: Long, val username: String)

data class TrackDto(
    val id: Long? = null,
    val deviceTrackId: Long,      // lokalni ID v telefonu, pro parovani
    val startedAt: Long,
    val endedAt: Long?,
    val state: String = "FINISHED",
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val movingTimeMillis: Long,
    val avgSpeedKmh: Double
)

data class PointDto(
    val deviceTrackId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMs: Float
)

data class LivePositionPing(val latitude: Double, val longitude: Double, val timestamp: Long)

data class UploadPointsRequest(val points: List<PointDto>)
data class UploadTrackResponse(val remoteId: Long)
data class TrackListResponse(val tracks: List<TrackDto>)
data class TrackDetailResponse(val track: TrackDto, val points: List<PointDto>)

interface ApiService {

    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("upload_track.php")
    suspend fun uploadTrack(@Body track: TrackDto): Response<UploadTrackResponse>

    @POST("upload_points.php")
    suspend fun uploadPoints(@Body body: UploadPointsRequest): Response<Unit>

    @GET("list_tracks.php")
    suspend fun listTracks(): Response<TrackListResponse>

    @GET("get_track.php")
    suspend fun getTrack(@Query("id") id: Long): Response<TrackDetailResponse>

    // Odesila se prubezne behem jizdy, aby web ukazoval "kde jsi ted".
    // Zamerne se nesynchronizuje pres WorkManager (neni kriticke, kdyz par pingu vypadne).
    @POST("ping_location.php")
    suspend fun pingLocation(@Body ping: LivePositionPing): Response<Unit>
}
