package org.librehu.widgets.source

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.librehu.widgets.data.VehicleData

/**
 * Where the vehicle data comes from. Each branch provides its implementation: `main` none (plain Android), `ivi`
 * Jancar ivi-services, `librehu-service` LibreHU-service. [DemoSource] simulates values.
 */
interface VehicleSource {
    /** Shown in the app ("Jancar ivi-services", …). */
    val name: String

    /** Starts delivering data to [onData] (main thread). */
    fun start(onData: (VehicleData) -> Unit)

    fun stop()

    companion object {
        fun create(context: Context): VehicleSource = if (isInstalled(context, "com.jancar.services")) IviSource(context) else NoSource

        private fun isInstalled(
            context: Context,
            pkg: String,
        ): Boolean =
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
    }
}

/** Plain Android device: nothing to read. */
object NoSource : VehicleSource {
    override val name = "Android"

    override fun start(onData: (VehicleData) -> Unit) = onData(VehicleData())

    override fun stop() {}
}

/** Cycling fake values to try the widgets and themes. */
class DemoSource : VehicleSource {
    override val name = "Demo"
    private val main = Handler(Looper.getMainLooper())
    private var tick = 0
    private var onData: (VehicleData) -> Unit = {}
    private val since = SystemClock.elapsedRealtime() - 37 * 60_000

    private val step =
        object : Runnable {
            override fun run() {
                tick++
                onData(
                    VehicleData(
                        linkOnline = true,
                        acc = true,
                        handbrake = (tick / 4) % 2 == 0,
                        headlights = (tick / 8) % 2 == 0,
                        reverse = tick % 10 == 9,
                        turnLeft = (tick / 2) % 3 == 0 && tick % 2 == 0,
                        turnRight = (tick / 2) % 3 == 1 && tick % 2 == 0,
                        mcuVersion = "JCST_AC8257_8T7-2024.08.09",
                        volume = 12 + tick % 6,
                        maxVolume = 40,
                        externalAmp = (tick / 6) % 2 == 0,
                        accSince = since,
                    ),
                )
                main.postDelayed(this, 1000)
            }
        }

    override fun start(onData: (VehicleData) -> Unit) {
        this.onData = onData
        main.post(step)
    }

    override fun stop() = main.removeCallbacks(step)
}
