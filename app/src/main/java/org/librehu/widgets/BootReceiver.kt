package org.librehu.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.librehu.widgets.widget.WidgetRenderer

/** Restarts the widget service after boot when widgets are placed. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (WidgetRenderer.hasWidgets(context)) WidgetService.start(context)
    }
}
