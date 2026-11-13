package com.example.antimarcipan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.antimarcipan.ui.AntimarcipanApp
import com.example.antimarcipan.ui.theme.AntimarcipanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) com.example.antimarcipan.ui.AppShortcuts.handleIntent(intent)
        // Vždy světlé ikony stavového řádku (aplikace má tmavé pozadí bez ohledu na režim telefonu)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            AntimarcipanTheme {
                Box(Modifier.fillMaxSize()) {
                    com.example.antimarcipan.ui.ThemedBackground()
                    AntimarcipanApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        com.example.antimarcipan.ui.AppShortcuts.handleIntent(intent)
    }
}
