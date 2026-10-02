package org.librehu.widgets.source

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.provider.Settings
import android.util.Log
import org.librehu.widgets.data.VehicleData

/**
 * Jancar ivi-services, polled every second with raw binder calls (reference: LibreHU-service
 * docs/ivi-services/api.md):
 * - ICar 11 getHandbrakeStatus (1 = engaged, -1 = unknown), 10 getCcdStatus (reverse), 13 getLightStatusMask
 *   (bit 0 / 1 = left / right turn signal), 14 getHeadLightStatus, 1 getProtocolMcuVersion;
 * - ignition: Settings.Global `accStatus` written by ivi-services (ICar.isAccOn always returns false);
 * - IAudio 8 getParam / 6 getParamMaxValue, parameter 10 = master volume;
 * - external amplifier: Settings.Global `external_amp_switch` ("External Power Amplifier Enable"), applied by
 *   ivi-services (MCU 0x44) while the ignition is on.
 */
class IviSource(
    private val context: Context,
) : VehicleSource {
    override val name = "Jancar ivi-services"

    private val car = Remote(context, "car", "com.jancar.services.car.ICar")
    private val audio = Remote(context, "audio", "com.jancar.services.audio.IAudio")
    private val thread = HandlerThread("ivi-poll").apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private var onData: (VehicleData) -> Unit = {}
    private var mcuVersion: String? = null
    private var tick = 0

    private val poll =
        object : Runnable {
            override fun run() {
                val d = read()
                main.post { onData(d) }
                worker.postDelayed(this, POLL_MS)
            }
        }

    override fun start(onData: (VehicleData) -> Unit) {
        this.onData = onData
        worker.postDelayed(poll, 500)
    }

    override fun stop() {
        worker.removeCallbacks(poll)
        thread.quitSafely()
        car.release()
        audio.release()
    }

    private fun read(): VehicleData {
        val acc =
            when (Settings.Global.getInt(context.contentResolver, "accStatus", -1)) {
                1 -> true
                0 -> false
                else -> null
            }
        val ampEnabled = Settings.Global.getInt(context.contentResolver, "external_amp_switch", 0) == 1
        val externalAmp = acc?.let { it && ampEnabled }
        if (!car.connected) return VehicleData(linkOnline = false, acc = acc, externalAmp = externalAmp)
        val handbrake = car.int(TX_HANDBRAKE)?.let { if (it < 0) null else it == 1 }
        val reverse = car.int(TX_CCD)?.let { if (it < 0) null else it == 1 }
        val lights = car.int(TX_LIGHT_MASK)
        val headlights = car.int(TX_HEADLIGHT)?.let { it != 0 }
        if (mcuVersion == null || tick++ % 30 == 0) car.string(TX_MCU_VERSION)?.let { mcuVersion = it.trim() }
        val volume = audio.int(TX_GET_PARAM, PARAM_VOLUME)
        val maxVolume = audio.int(TX_GET_PARAM_MAX, PARAM_VOLUME)
        return VehicleData(
            linkOnline = true,
            acc = acc,
            handbrake = handbrake,
            headlights = headlights,
            reverse = reverse,
            turnLeft = lights?.let { it and 1 != 0 },
            turnRight = lights?.let { it and 2 != 0 },
            mcuVersion = mcuVersion,
            volume = volume,
            maxVolume = maxVolume,
            externalAmp = externalAmp,
        )
    }

    /** One bound ivi-services interface. */
    private class Remote(
        private val context: Context,
        service: String,
        private val descriptor: String,
    ) {
        @Volatile
        private var binder: IBinder? = null
        private var bound = false
        val connected: Boolean get() = binder != null

        private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName?,
                    service: IBinder?,
                ) {
                    binder = service
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    binder = null
                }
            }

        init {
            bound =
                try {
                    context.bindService(
                        Intent("com.jancar.services.action.$service").setPackage("com.jancar.services"),
                        connection,
                        Context.BIND_AUTO_CREATE,
                    )
                } catch (e: SecurityException) {
                    Log.w(TAG, "ivi-services $service: ${e.message}")
                    false
                }
        }

        fun int(
            code: Int,
            arg: Int? = null,
        ): Int? = call(code, arg) { it.readInt() }

        fun string(code: Int): String? = call(code, null) { it.readString() }

        private fun <T> call(
            code: Int,
            arg: Int?,
            read: (Parcel) -> T,
        ): T? {
            val b = binder ?: return null
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(descriptor)
                if (arg != null) data.writeInt(arg)
                b.transact(code, data, reply, 0)
                reply.readException()
                read(reply)
            } catch (e: Exception) {
                Log.w(TAG, "$descriptor $code: ${e.message}")
                null
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

        fun release() {
            if (bound) {
                try {
                    context.unbindService(connection)
                } catch (_: IllegalArgumentException) {
                }
            }
            bound = false
        }
    }

    private companion object {
        const val TAG = "LibreHU-Widgets"
        const val POLL_MS = 1000L
        const val TX_MCU_VERSION = 1
        const val TX_CCD = 10
        const val TX_HANDBRAKE = 11
        const val TX_LIGHT_MASK = 13
        const val TX_HEADLIGHT = 14
        const val TX_GET_PARAM_MAX = 6
        const val TX_GET_PARAM = 8
        const val PARAM_VOLUME = 10
    }
}
