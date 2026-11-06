package com.example.antimarcipan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.antimarcipan.ui.AntimarcipanApp
import com.example.antimarcipan.ui.theme.AntimarcipanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AntimarcipanTheme {
                AntimarcipanApp()
            }
        }
    }
}
