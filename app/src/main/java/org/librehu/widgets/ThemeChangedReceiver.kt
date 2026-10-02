package org.librehu.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.librehu.widgets.widget.WidgetRenderer

/** LibreHU Launcher announces a theme change: make sure the widget service runs and redraws. */
class ThemeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (WidgetRenderer.hasWidgets(context)) WidgetService.start(context)
    }
}
