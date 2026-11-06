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
        enableEdgeToEdge()
        setContent {
            AntimarcipanTheme {
                Box(Modifier.fillMaxSize()) {
                    com.example.antimarcipan.ui.ThemedBackground()
                    AntimarcipanApp()
                }
            }
        }
    }
}
