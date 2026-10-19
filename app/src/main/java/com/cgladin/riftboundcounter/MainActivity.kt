package com.cgladin.riftboundcounter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cgladin.riftboundcounter.ui.CounterRoute

/**
 * Activité principale qui affiche le compteur et garde l’écran allumé pendant une partie.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { CounterRoute() }
    }
}
