package com.example.aapremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aapremote.navigation.AppNavigation
import com.example.aapremote.ui.theme.AapRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AapRemoteTheme {
                AppNavigation()
            }
        }
    }
}
