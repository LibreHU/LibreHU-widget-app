package org.librehu.widgets.bridge

import android.content.Context
import org.librehu.widgets.R
import org.librehu.widgets.data.Probe
import org.librehu.widgets.data.SettingsStore
import org.librehu.widgets.data.ThemeMode
import org.librehu.widgets.widget.WidgetRenderer

/**
 * The widgets' settings for LibreHU's phone app (through LibreHU-service's bridge, [BridgeKitProvider]): each place of
 * the indicators and info widgets (a value or nothing), labels, trip time, theme, demo. Same labels and same store as
 * the app's screen.
 */
class BridgeProvider : BridgeKitProvider() {
    override fun pages(context: Context): List<KitPage> {
        val store = SettingsStore.get(context)
        val st = store.settings.value
        return listOf(
            page(context, "widgets", context.getString(R.string.app_name), "widgets") {
                section(s(R.string.section_indicators))
                slots("indicator", st.indicators, Probe.STATES, WidgetRenderer.MAX_INDICATORS) { list -> store.update { it.copy(indicators = list) } }
                section(s(R.string.section_info))
                slots("info", st.infoRows, Probe.entries, WidgetRenderer.MAX_INFO_ROWS) { list -> store.update { it.copy(infoRows = list) } }
                section(s(R.string.section_display))
                switch("labels", s(R.string.show_labels), st.showLabels) { v -> store.update { it.copy(showLabels = v) } }
                switch("clock_trip", s(R.string.clock_trip), st.clockShowsTrip) { v -> store.update { it.copy(clockShowsTrip = v) } }
                section(s(R.string.section_theme))
                pick("theme", s(R.string.section_theme), st.theme, s(R.string.theme_follow_hint), label = {
                    s(
                        when (it) {
                            ThemeMode.FOLLOW -> R.string.theme_follow
                            ThemeMode.HEADLIGHTS -> R.string.theme_headlights
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        },
                    )
                }) { v -> store.update { it.copy(theme = v) } }
                switch("demo", s(R.string.demo), st.demo, s(R.string.demo_hint)) { v -> store.update { it.copy(demo = v) } }
            },
        )
    }

    /** One choice per place of a widget: a value among [offered], or nothing (the places after it move up). */
    private fun KitBuilder.slots(
        id: String,
        list: List<Probe>,
        offered: List<Probe>,
        max: Int,
        save: (List<Probe>) -> Unit,
    ) {
        val none = "-"
        val options = listOf(none to "—") + offered.map { it.name to s(it.label) }
        for (i in 0 until max) {
            choice("${id}_$i", "${i + 1}", list.getOrNull(i)?.name ?: none, options) { v ->
                val next = list.toMutableList()
                val p = offered.firstOrNull { it.name == v }
                when {
                    p == null -> if (i < next.size) next.removeAt(i)
                    i < next.size -> next[i] = p
                    else -> next += p
                }
                save(next.distinct())
            }
        }
    }
}
