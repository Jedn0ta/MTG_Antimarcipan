package com.example.antimarcipan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.antimarcipan.ui.AntimarcipanApp
import com.example.antimarcipan.ui.theme.AntimarcipanTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AntimarcipanTheme {
                Box(Modifier.fillMaxSize()) {
                    AntimarcipanApp()
                    StartupSplash()
                }
            }
        }
    }
}

/** Krátký úvodní splash s ikonou a číslem verze. */
@Composable
private fun StartupSplash() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1600)
        visible = false
    }
    AnimatedVisibility(visible = visible, exit = fadeOut()) {
        Surface(color = Color(0xFF241812), modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_icon),
                    contentDescription = null,
                    modifier = Modifier.size(220.dp),
                )
                Text(
                    "Antimarcipán",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFE9C46A),
                )
                Text(
                    "verze ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE9C46A).copy(alpha = 0.8f),
                )
            }
        }
    }
}
