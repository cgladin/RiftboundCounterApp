package com.cgladin.riftboundcounter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cgladin.riftboundcounter.ui.CounterRoute

/**
 * Main activity that displays the counter and keeps the screen on during a game.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { CounterRoute() }
    }
}
