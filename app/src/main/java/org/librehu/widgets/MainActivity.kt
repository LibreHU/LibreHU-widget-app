package org.librehu.widgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.librehu.widgets.ui.CarTheme
import org.librehu.widgets.ui.SettingsScreen

/** Settings app: live values from the head unit and what each widget shows. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetService.start(this)
        setContent { CarTheme { SettingsScreen() } }
    }

    override fun onDestroy() {
        WidgetService.start(this, WidgetService.ACTION_STOP_IF_UNUSED)
        super.onDestroy()
    }
}
