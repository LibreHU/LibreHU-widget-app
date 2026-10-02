package org.librehu.widgets.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import org.librehu.widgets.WidgetService
import org.librehu.widgets.data.SettingsStore

/** Common behaviour: the service draws every widget; it stops when the last widget is removed. */
abstract class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) = WidgetService.start(context)

    override fun onEnabled(context: Context) = WidgetService.start(context)

    override fun onDisabled(context: Context) = WidgetService.start(context, WidgetService.ACTION_STOP_IF_UNUSED)
}

/** Row of indicator lamps (handbrake, lights, ignition, reverse, turn signals…). */
class IndicatorsWidget : BaseWidget()

/** Time, date and trip time since the ignition was switched on. */
class ClockWidget : BaseWidget()

/** List of values (ignition, handbrake, trip time, volume, MCU version…). */
class InfoWidget : BaseWidget()

/** One big lamp or value, chosen when the widget is placed. */
class SingleWidget : BaseWidget() {
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        val store = SettingsStore.get(context)
        appWidgetIds.forEach(store::forgetWidget)
    }
}
