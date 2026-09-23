package cz.example.gpstracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val MAP_STYLE_URL =
    "https://tiles.openfreemap.org/styles/liberty"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GpsTrackerMapScreen()
        }
    }
}

@Composable
private fun GpsTrackerMapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasLocationPermission by remember {
        mutableStateOf(
            hasLocationPermission(context)
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            hasLocationPermission = hasLocationPermission(context)
        }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val mapView = remember {
        MapView(context)
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                onCreate(null)

                getMapAsync { mapLibreMap ->
                    mapLibreMap.setStyle(
                        Style.Builder()
                            .fromUri(MAP_STYLE_URL)
                    ) { style ->
                        mapLibreMap.setCameraPosition(
                            CameraPosition.Builder()
                                .zoom(15.0)
                                .build()
                        )

                        if (hasLocationPermission(context)) {
                            enableUserLocation(
                                mapLibreMap = mapLibreMap,
                                style = style,
                                mapView = this
                            )
                        }
                    }
                }
            }
        },
        update = {
            if (hasLocationPermission) {
                it.getMapAsync { mapLibreMap ->
                    mapLibreMap.getStyle { style ->
                        if (style != null) {
                            enableUserLocation(
                                mapLibreMap = mapLibreMap,
                                style = style,
                                mapView = it
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun enableUserLocation(
    mapLibreMap: MapLibreMap,
    style: Style,
    mapView: MapView
) {
    if (!hasLocationPermission(mapView.context)) {
        return
    }

    val locationComponent = mapLibreMap.locationComponent

    val activationOptions =
        LocationComponentActivationOptions
            .builder(mapView.context, style)
            .useDefaultLocationEngine(true)
            .build()

    locationComponent.activateLocationComponent(activationOptions)
    locationComponent.isLocationComponentEnabled = true

    /*
     * TRACKING_COMPASS:
     * - mapa sleduje aktuální polohu,
     * - mapa se natáčí podle směru telefonu.
     */
    locationComponent.cameraMode = CameraMode.TRACKING_COMPASS

    /*
     * COMPASS:
     * - zobrazí směrovou šipku,
     * - MapLibre zpracuje chvění kompasu.
     */
    locationComponent.renderMode = RenderMode.COMPASS

    locationComponent.zoomWhileTracking(15.0)
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fineLocationGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    return fineLocationGranted || coarseLocationGranted
}