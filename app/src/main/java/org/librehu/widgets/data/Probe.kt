package org.librehu.widgets.data

import android.content.Context
import android.os.SystemClock
import org.librehu.widgets.R

/** What the head unit reports. Null = unknown / not provided by the current source. */
data class VehicleData(
    val linkOnline: Boolean? = null,
    val acc: Boolean? = null,
    val handbrake: Boolean? = null,
    val headlights: Boolean? = null,
    val reverse: Boolean? = null,
    val turnLeft: Boolean? = null,
    val turnRight: Boolean? = null,
    val mcuVersion: String? = null,
    val volume: Int? = null,
    val maxVolume: Int? = null,
    /** External amplifier remote (REM) output powered: setting enabled and ignition on. */
    val externalAmp: Boolean? = null,
    /** SystemClock.elapsedRealtime() when the ignition was switched on (trip timer), null when off / unknown. */
    val accSince: Long? = null,
)

enum class ProbeKind { STATE, TEXT }

/**
 * A "probe": one value a widget can show. STATE probes are indicator lamps (icon lit in [activeColor] when on),
 * TEXT probes are values.
 */
enum class Probe(
    val kind: ProbeKind,
    val label: Int,
    val icon: Int,
    val activeColor: Int,
) {
    HANDBRAKE(ProbeKind.STATE, R.string.probe_handbrake, R.drawable.ic_probe_handbrake, 0xFFF44336.toInt()),
    HEADLIGHTS(ProbeKind.STATE, R.string.probe_headlights, R.drawable.ic_probe_headlights, 0xFF4CAF50.toInt()),
    ACC(ProbeKind.STATE, R.string.probe_acc, R.drawable.ic_probe_acc, 0xFF4CAF50.toInt()),
    REVERSE(ProbeKind.STATE, R.string.probe_reverse, R.drawable.ic_probe_reverse, 0xFFFFFFFF.toInt()),
    TURN_LEFT(ProbeKind.STATE, R.string.probe_turn_left, R.drawable.ic_probe_turn_left, 0xFF4CAF50.toInt()),
    TURN_RIGHT(ProbeKind.STATE, R.string.probe_turn_right, R.drawable.ic_probe_turn_right, 0xFF4CAF50.toInt()),
    LINK(ProbeKind.STATE, R.string.probe_link, R.drawable.ic_probe_link, 0xFF8AB4F8.toInt()),
    EXT_AMP(ProbeKind.STATE, R.string.probe_ext_amp, R.drawable.ic_probe_amp, 0xFFFFB300.toInt()),
    MCU_VERSION(ProbeKind.TEXT, R.string.probe_mcu_version, R.drawable.ic_probe_link, 0),
    VOLUME(ProbeKind.TEXT, R.string.probe_volume, R.drawable.ic_probe_volume, 0),
    TRIP(ProbeKind.TEXT, R.string.probe_trip, R.drawable.ic_probe_trip, 0),
    ;

    fun state(d: VehicleData): Boolean? =
        when (this) {
            HANDBRAKE -> d.handbrake
            HEADLIGHTS -> d.headlights
            ACC -> d.acc
            REVERSE -> d.reverse
            TURN_LEFT -> d.turnLeft
            TURN_RIGHT -> d.turnRight
            LINK -> d.linkOnline
            EXT_AMP -> d.externalAmp
            else -> null
        }

    /** Text shown for this probe ("—" when unknown). */
    fun text(
        context: Context,
        d: VehicleData,
    ): String =
        when (this) {
            MCU_VERSION -> {
                d.mcuVersion?.ifBlank { null } ?: DASH
            }

            VOLUME -> {
                d.volume?.let { v -> d.maxVolume?.let { "$v / $it" } ?: "$v" } ?: DASH
            }

            TRIP -> {
                d.accSince?.let { formatDuration(SystemClock.elapsedRealtime() - it) } ?: DASH
            }

            else -> {
                when (state(d)) {
                    true -> context.getString(if (this == HANDBRAKE) R.string.state_engaged else R.string.state_on)
                    false -> context.getString(if (this == HANDBRAKE) R.string.state_released else R.string.state_off)
                    null -> DASH
                }
            }
        }

    companion object {
        const val DASH = "—"
        val STATES = entries.filter { it.kind == ProbeKind.STATE }

        fun formatDuration(ms: Long): String {
            val m = (ms / 60_000).coerceAtLeast(0)
            return "%d:%02d".format(m / 60, m % 60)
        }
    }
}
