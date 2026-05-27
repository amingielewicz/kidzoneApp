package com.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.playground.navigation.PlaygroundNavGraph
import com.playground.ui.theme.PlaygroundTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Jedyna aktywność aplikacji – host dla całej hierarchii Compose.
 *
 * Adnotacja [AndroidEntryPoint] włącza wstrzykiwanie zależności przez Hilt
 * w aktywności i zagnieżdżonych w niej @HiltViewModel-ach.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaygroundTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaygroundNavGraph()
                }
            }
        }
    }
}
