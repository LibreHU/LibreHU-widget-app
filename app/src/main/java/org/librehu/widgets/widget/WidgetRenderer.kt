package org.librehu.widgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import org.librehu.widgets.MainActivity
import org.librehu.widgets.R
import org.librehu.widgets.data.Probe
import org.librehu.widgets.data.SettingsStore
import org.librehu.widgets.data.VehicleData
import org.librehu.widgets.data.WidgetSettings

/** Draws every widget of the app (RemoteViews) from the vehicle data, settings and theme. */
object WidgetRenderer {
    private class Colors(
        dark: Boolean,
        accentArgb: Int,
    ) {
        /** Accent of LibreHU Launcher (0 = none: the default blue). */
        val accent =
            if (accentArgb != 0) {
                accentArgb
            } else if (dark) {
                0xFF8AB4F8.toInt()
            } else {
                0xFF1A73E8.toInt()
            }
        val background = if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
        val text = if (dark) 0xFFE8EAED.toInt() else 0xFF202124.toInt()
        val dim = if (dark) 0xFF9AA0A6.toInt() else 0xFF5F6368.toInt()
        val off = if (dark) 0xFF5F6368.toInt() else 0xFFBDC1C6.toInt()
        val unknown = if (dark) 0xFF35363A.toInt() else 0xFFE8EAED.toInt()
        val isDark = dark
    }

    private val INDICATOR_SLOTS =
        listOf(
            Triple(R.id.slot0, R.id.icon0, R.id.label0),
            Triple(R.id.slot1, R.id.icon1, R.id.label1),
            Triple(R.id.slot2, R.id.icon2, R.id.label2),
            Triple(R.id.slot3, R.id.icon3, R.id.label3),
            Triple(R.id.slot4, R.id.icon4, R.id.label4),
            Triple(R.id.slot5, R.id.icon5, R.id.label5),
            Triple(R.id.slot6, R.id.icon6, R.id.label6),
            Triple(R.id.slot7, R.id.icon7, R.id.label7),
        )

    private val INFO_ROWS =
        listOf(
            arrayOf(R.id.row0, R.id.row_icon0, R.id.row_label0, R.id.row_value0),
            arrayOf(R.id.row1, R.id.row_icon1, R.id.row_label1, R.id.row_value1),
            arrayOf(R.id.row2, R.id.row_icon2, R.id.row_label2, R.id.row_value2),
            arrayOf(R.id.row3, R.id.row_icon3, R.id.row_label3, R.id.row_value3),
            arrayOf(R.id.row4, R.id.row_icon4, R.id.row_label4, R.id.row_value4),
            arrayOf(R.id.row5, R.id.row_icon5, R.id.row_label5, R.id.row_value5),
        )

    val MAX_INDICATORS = INDICATOR_SLOTS.size
    val MAX_INFO_ROWS = INFO_ROWS.size

    private val PROVIDERS =
        listOf(IndicatorsWidget::class.java, ClockWidget::class.java, InfoWidget::class.java, SingleWidget::class.java)

    fun hasWidgets(context: Context): Boolean = PROVIDERS.any { ids(context, it).isNotEmpty() }

    fun updateAll(
        context: Context,
        d: VehicleData,
        s: WidgetSettings,
        dark: Boolean,
        accent: Int = 0,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val c = Colors(dark, accent)
        ids(
            context,
            IndicatorsWidget::class.java,
        ).takeIf { it.isNotEmpty() }?.let { manager.updateAppWidget(it, indicators(context, d, s, c)) }
        ids(context, ClockWidget::class.java).takeIf { it.isNotEmpty() }?.let { manager.updateAppWidget(it, clock(context, d, s, c)) }
        ids(context, InfoWidget::class.java).takeIf { it.isNotEmpty() }?.let { manager.updateAppWidget(it, info(context, d, s, c)) }
        val store = SettingsStore.get(context)
        for (id in ids(context, SingleWidget::class.java)) manager.updateAppWidget(id, single(context, d, s, c, store.singleProbe(id)))
    }

    private fun ids(
        context: Context,
        cls: Class<*>,
    ): IntArray = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    /** Lamp colour: lit in the probe colour, dim when off, very dim when unknown. */
    private fun lampColor(
        p: Probe,
        state: Boolean?,
        c: Colors,
    ): Int =
        when (state) {
            true -> if (p == Probe.REVERSE && !c.isDark) 0xFF202124.toInt() else p.activeColor
            false -> c.off
            null -> c.unknown
        }

    private fun indicators(
        context: Context,
        d: VehicleData,
        s: WidgetSettings,
        c: Colors,
    ) = RemoteViews(context.packageName, R.layout.widget_indicators).apply {
        setInt(R.id.root, "setBackgroundResource", c.background)
        setOnClickPendingIntent(R.id.root, openApp(context))
        val probes = s.indicators.filter { it.kind == org.librehu.widgets.data.ProbeKind.STATE }
        INDICATOR_SLOTS.forEachIndexed { i, (slot, icon, label) ->
            val p = probes.getOrNull(i)
            if (p == null) {
                setViewVisibility(slot, View.GONE)
                return@forEachIndexed
            }
            val state = p.state(d)
            setViewVisibility(slot, View.VISIBLE)
            setImageViewResource(icon, p.icon)
            setInt(icon, "setColorFilter", lampColor(p, state, c))
            setContentDescription(icon, context.getString(p.label) + " " + p.text(context, d))
            setViewVisibility(label, if (s.showLabels) View.VISIBLE else View.GONE)
            setTextViewText(label, context.getString(p.label))
            setTextColor(label, if (state == true) c.text else c.dim)
        }
    }

    private fun clock(
        context: Context,
        d: VehicleData,
        s: WidgetSettings,
        c: Colors,
    ) = RemoteViews(context.packageName, R.layout.widget_clock).apply {
        setInt(R.id.root, "setBackgroundResource", c.background)
        setOnClickPendingIntent(R.id.root, openApp(context))
        setTextColor(R.id.clock_time, c.accent)
        setTextColor(R.id.clock_date, c.dim)
        val since = d.accSince
        if (s.clockShowsTrip && since != null) {
            setViewVisibility(R.id.clock_trip_row, View.VISIBLE)
            setInt(R.id.clock_trip_icon, "setColorFilter", c.accent)
            setTextColor(R.id.clock_trip, c.dim)
            // The chronometer runs by itself: no redraw needed every second.
            setChronometer(R.id.clock_trip, since, null, true)
        } else {
            setViewVisibility(R.id.clock_trip_row, View.GONE)
        }
    }

    private fun info(
        context: Context,
        d: VehicleData,
        s: WidgetSettings,
        c: Colors,
    ) = RemoteViews(context.packageName, R.layout.widget_info).apply {
        setInt(R.id.root, "setBackgroundResource", c.background)
        setOnClickPendingIntent(R.id.root, openApp(context))
        setTextColor(R.id.info_title, c.accent)
        INFO_ROWS.forEachIndexed { i, ids ->
            val p = s.infoRows.getOrNull(i)
            if (p == null) {
                setViewVisibility(ids[0], View.GONE)
                return@forEachIndexed
            }
            setViewVisibility(ids[0], View.VISIBLE)
            setImageViewResource(ids[1], p.icon)
            setInt(
                ids[1],
                "setColorFilter",
                if (p.kind ==
                    org.librehu.widgets.data.ProbeKind.STATE
                ) {
                    lampColor(p, p.state(d), c)
                } else {
                    c.accent
                },
            )
            setTextViewText(ids[2], context.getString(p.label))
            setTextColor(ids[2], c.dim)
            setTextViewText(ids[3], p.text(context, d))
            setTextColor(ids[3], c.text)
        }
    }

    private fun single(
        context: Context,
        d: VehicleData,
        s: WidgetSettings,
        c: Colors,
        p: Probe,
    ) = RemoteViews(context.packageName, R.layout.widget_single).apply {
        setInt(R.id.root, "setBackgroundResource", c.background)
        setOnClickPendingIntent(R.id.root, openApp(context))
        setImageViewResource(R.id.single_icon, p.icon)
        val color = if (p.kind == org.librehu.widgets.data.ProbeKind.STATE) lampColor(p, p.state(d), c) else c.accent
        setInt(R.id.single_icon, "setColorFilter", color)
        setTextViewText(
            R.id.single_label,
            if (p.kind ==
                org.librehu.widgets.data.ProbeKind.STATE
            ) {
                context.getString(p.label)
            } else {
                p.text(context, d)
            },
        )
        setTextColor(R.id.single_label, c.dim)
        setViewVisibility(
            R.id.single_label,
            if (s.showLabels ||
                p.kind == org.librehu.widgets.data.ProbeKind.TEXT
            ) {
                View.VISIBLE
            } else {
                View.GONE
            },
        )
    }
}
