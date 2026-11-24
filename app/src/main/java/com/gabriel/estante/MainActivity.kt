package com.gabriel.estante

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gabriel.estante.ui.navigation.EstanteNavHost
import com.gabriel.estante.ui.theme.EstanteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EstanteTheme {
                EstanteNavHost()
            }
        }
    }
}
