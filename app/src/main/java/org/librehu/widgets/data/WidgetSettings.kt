package org.librehu.widgets.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { FOLLOW, HEADLIGHTS, LIGHT, DARK }

data class WidgetSettings(
    /** Indicator lamps of the "indicators" widget, in order. */
    val indicators: List<Probe> = listOf(Probe.HANDBRAKE, Probe.HEADLIGHTS, Probe.ACC, Probe.REVERSE, Probe.TURN_LEFT, Probe.TURN_RIGHT),
    /** Rows of the "info" widget, in order. */
    val infoRows: List<Probe> = listOf(Probe.ACC, Probe.HANDBRAKE, Probe.EXT_AMP, Probe.TRIP, Probe.VOLUME, Probe.MCU_VERSION),
    val showLabels: Boolean = true,
    val clockShowsTrip: Boolean = true,
    val theme: ThemeMode = ThemeMode.FOLLOW,
    /** Fake values cycling, to try the widgets away from the car. */
    val demo: Boolean = false,
)

class SettingsStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<WidgetSettings> = _settings.asStateFlow()

    fun update(transform: (WidgetSettings) -> WidgetSettings) {
        val s = transform(_settings.value)
        prefs
            .edit()
            .putString("indicators", s.indicators.joinToString(",") { it.name })
            .putString("info_rows", s.infoRows.joinToString(",") { it.name })
            .putBoolean("labels", s.showLabels)
            .putBoolean("clock_trip", s.clockShowsTrip)
            .putString("theme", s.theme.name)
            .putBoolean("demo", s.demo)
            .apply()
        _settings.value = s
    }

    /** Probe shown by a single-probe widget. */
    fun singleProbe(appWidgetId: Int): Probe = parse(prefs.getString("single_$appWidgetId", null)).firstOrNull() ?: Probe.HANDBRAKE

    fun setSingleProbe(
        appWidgetId: Int,
        probe: Probe,
    ) = prefs.edit().putString("single_$appWidgetId", probe.name).apply()

    fun forgetWidget(appWidgetId: Int) = prefs.edit().remove("single_$appWidgetId").apply()

    private fun load(): WidgetSettings {
        val d = WidgetSettings()
        return WidgetSettings(
            indicators = prefs.getString("indicators", null)?.let(::parse) ?: d.indicators,
            infoRows = prefs.getString("info_rows", null)?.let(::parse) ?: d.infoRows,
            showLabels = prefs.getBoolean("labels", d.showLabels),
            clockShowsTrip = prefs.getBoolean("clock_trip", d.clockShowsTrip),
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(d.theme),
            demo = prefs.getBoolean("demo", d.demo),
        )
    }

    private fun parse(s: String?): List<Probe> =
        s
            ?.split(',')
            ?.mapNotNull { n -> runCatching { Probe.valueOf(n) }.getOrNull() }
            .orEmpty()

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
