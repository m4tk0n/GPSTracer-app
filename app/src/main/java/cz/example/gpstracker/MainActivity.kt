package cz.example.gpstracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GPSTrackerApp()
        }
    }
}

@Composable
fun GPSTrackerApp() {
    val context = LocalContext.current

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            errorMessage = getLocationError(context)
        }

    fun checkLocation() {
        errorMessage = getLocationError(context)
    }

    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        checkLocation()
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                MainContent(
                    onTestError = {
                        errorMessage = "Testovací chyba aplikace."
                    },
                    onCheckLocation = {
                        checkLocation()
                    },
                    onRequestPermission = {
                        requestLocationPermission()
                    }
                )

                errorMessage?.let { message ->
                    ErrorBanner(
                        message = message,
                        onClose = {
                            errorMessage = null
                        },
                        onRequestPermission = {
                            requestLocationPermission()
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

private fun getLocationError(context: Context): String? {
    val hasFineLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    if (!hasFineLocationPermission && !hasCoarseLocationPermission) {
        return "Aplikace nemá povolení k poloze."
    }

    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val gpsEnabled = try {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (_: Exception) {
        false
    }

    val networkEnabled = try {
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (_: Exception) {
        false
    }

    if (!gpsEnabled && !networkEnabled) {
        return "Poloha zařízení je vypnutá."
    }

    return null
}

@Composable
private fun MainContent(
    onTestError: () -> Unit,
    onCheckLocation: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GPS Tracker",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Základní verze aplikace",
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(text = "Povolit polohu")
        }

        Button(
            onClick = onCheckLocation,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(text = "Zkontrolovat GPS")
        }

        Button(
            onClick = onTestError,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(text = "Otestovat chybový pruh")
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFD32F2F))
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = 8.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        if (message.contains("povolení")) {
            Button(
                onClick = onRequestPermission
            ) {
                Text(text = "Povolit")
            }
        }

        Button(
            onClick = onClose
        ) {
            Text(text = "Zavřít")
        }
    }
}
