package org.librehu.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librehu.widgets.R
import org.librehu.widgets.WidgetService
import org.librehu.widgets.data.Probe
import org.librehu.widgets.data.ProbeKind
import org.librehu.widgets.data.SettingsStore
import org.librehu.widgets.data.ThemeMode
import org.librehu.widgets.data.VehicleData
import org.librehu.widgets.widget.WidgetRenderer

/** Follows the widgets' effective theme (launcher, headlights or forced). */
@Composable
private fun FollowWidgetTheme() {
    val dark by WidgetService.dark.collectAsStateWithLifecycle()
    LaunchedEffect(dark) { CarColors.palette = CarPalette.of(dark) }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    FollowWidgetTheme()
    val context = LocalContext.current
    val store = SettingsStore.get(context)
    val s by store.settings.collectAsStateWithLifecycle()
    val d by WidgetService.data.collectAsStateWithLifecycle()
    val source by WidgetService.sourceName.collectAsStateWithLifecycle()
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CarColors.Background)
                .safeDrawingPadding()
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Text(stringResource(R.string.live), color = CarColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.source, source), color = CarColors.TextDim, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Probe.entries.forEach { p -> LiveTile(p, d) }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.unknown_hint), color = CarColors.TextDim, fontSize = 13.sp)
        }
        Card(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Section(R.string.section_indicators)
            Hint(stringResource(R.string.section_indicators_hint, WidgetRenderer.MAX_INDICATORS))
            ProbeList(Probe.STATES, s.indicators, WidgetRenderer.MAX_INDICATORS) { list -> store.update { it.copy(indicators = list) } }

            Section(R.string.section_info)
            Hint(stringResource(R.string.section_info_hint, WidgetRenderer.MAX_INFO_ROWS))
            ProbeList(Probe.entries, s.infoRows, WidgetRenderer.MAX_INFO_ROWS) { list -> store.update { it.copy(infoRows = list) } }

            Section(R.string.section_display)
            SwitchRow(stringResource(R.string.show_labels), null, s.showLabels) { on -> store.update { it.copy(showLabels = on) } }
            SwitchRow(stringResource(R.string.clock_trip), null, s.clockShowsTrip) { on -> store.update { it.copy(clockShowsTrip = on) } }

            Section(R.string.section_theme)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    ThemeMode.FOLLOW to R.string.theme_follow,
                    ThemeMode.HEADLIGHTS to R.string.theme_headlights,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                ).forEach { (mode, label) ->
                    Choice(stringResource(label), s.theme == mode) { store.update { it.copy(theme = mode) } }
                }
            }
            Hint(stringResource(R.string.theme_follow_hint))

            Section(R.string.demo)
            SwitchRow(
                stringResource(R.string.demo),
                stringResource(R.string.demo_hint),
                s.demo,
            ) { on -> store.update { it.copy(demo = on) } }
            Spacer(Modifier.height(12.dp))
            Hint(stringResource(R.string.add_hint))
        }
    }
}

@Composable
private fun LiveTile(
    p: Probe,
    d: VehicleData,
) {
    val context = LocalContext.current
    val state = p.state(d)
    val tint =
        when {
            p.kind == ProbeKind.TEXT -> CarColors.TextDim
            state == true -> if (p == Probe.REVERSE && !CarColors.palette.dark) CarColors.Text else Color(p.activeColor)
            state == false -> CarColors.TextDim.copy(alpha = 0.5f)
            else -> CarColors.SurfaceHigh
        }
    Column(
        modifier =
            Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CarColors.SurfaceHigh.copy(alpha = 0.5f))
                .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(p.icon), null, tint = tint, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(p.label), color = CarColors.TextDim, fontSize = 13.sp, maxLines = 1)
        Text(p.text(context, d), color = CarColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Checkable list of probes; the selected ones can be reordered. */
@Composable
private fun ProbeList(
    all: List<Probe>,
    selected: List<Probe>,
    max: Int,
    onChange: (List<Probe>) -> Unit,
) {
    val ordered = selected + all.filterNot { it in selected }
    ordered.forEach { p ->
        val checked = p in selected
        val index = selected.indexOf(p)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (checked) {
                            onChange(selected - p)
                        } else if (selected.size < max) {
                            onChange(selected + p)
                        }
                    }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = CarColors.Accent, checkmarkColor = CarColors.OnAccent),
            )
            Icon(painterResource(p.icon), null, tint = CarColors.TextDim, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(p.label), color = CarColors.Text, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (checked) {
                IconButton(onClick = { onChange(move(selected, index, -1)) }, enabled = index > 0) {
                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up), tint = CarColors.Text)
                }
                IconButton(onClick = { onChange(move(selected, index, 1)) }, enabled = index < selected.size - 1) {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down), tint = CarColors.Text)
                }
            }
        }
    }
}

private fun move(
    list: List<Probe>,
    i: Int,
    offset: Int,
): List<Probe> {
    val j = i + offset
    if (i < 0 || j !in list.indices) return list
    return list.toMutableList().apply { add(j, removeAt(i)) }
}

/** Widget configuration: pick one probe. */
@Composable
fun ProbePicker(onPick: (Probe) -> Unit) {
    FollowWidgetTheme()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CarColors.Background)
                .safeDrawingPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.pick_probe), color = CarColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Probe.entries.forEach { p ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarColors.Surface)
                        .clickable { onPick(p) }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(p.icon),
                    null,
                    tint =
                        if (p.activeColor !=
                            0
                        ) {
                            Color(p.activeColor)
                        } else {
                            CarColors.Accent
                        },
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(stringResource(p.label), color = CarColors.Text, fontSize = 19.sp)
            }
        }
    }
}

@Composable
private fun Card(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(28.dp))
                .background(CarColors.Surface)
                .padding(20.dp),
    ) { content() }
}

@Composable
private fun Section(title: Int) {
    Text(
        stringResource(title),
        color = CarColors.Accent,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, color = CarColors.TextDim, fontSize = 14.sp)
}

@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        label,
        color = if (selected) CarColors.OnAccent else CarColors.Text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) CarColors.Accent else CarColors.SurfaceHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = CarColors.Text, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = CarColors.TextDim, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = CarColors.Accent, checkedThumbColor = CarColors.OnAccent),
        )
    }
}
