package com.example.antimarcipan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.antimarcipan.ui.AntimarcipanApp
import com.example.antimarcipan.ui.theme.AntimarcipanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AntimarcipanTheme {
                AntimarcipanApp()
            }
        }
    }
}
