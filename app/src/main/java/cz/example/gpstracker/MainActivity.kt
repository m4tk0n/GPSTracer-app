package cz.example.gpstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    var errorMessage by remember {
        mutableStateOf<String?>(null)
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
                    }
                )

                errorMessage?.let { message ->
                    ErrorBanner(
                        message = message,
                        onClose = {
                            errorMessage = null
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    onTestError: () -> Unit
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
            onClick = onTestError,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(text = "Otestovat chybový pruh")
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onClose: () -> Unit,
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

        Button(
            onClick = onClose
        ) {
            Text(text = "Zavřít")
        }
    }
}