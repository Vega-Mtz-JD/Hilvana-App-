package com.hilvana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hilvana.app.ui.navigation.HilvanaNavGraph
import com.hilvana.app.ui.theme.HilvanaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HilvanaTheme {
                HilvanaNavGraph()
            }
        }
    }
}
