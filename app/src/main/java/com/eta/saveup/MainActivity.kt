package com.eta.saveup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.eta.saveup.ui.SaveUpApp
import com.eta.saveup.ui.theme.SaveUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaveUpTheme(darkTheme = isSystemInDarkTheme()) {
                SaveUpApp()
            }
        }
    }
}
