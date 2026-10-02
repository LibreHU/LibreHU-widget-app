package org.librehu.widgets.source

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import org.librehu.service.ILibreHuCallback
import org.librehu.service.ILibreHuService
import org.librehu.widgets.data.VehicleData

/**
 * LibreHU-service (https://github.com/LibreHU/LibreHU-service): vehicle flags (MCU link, ignition, handbrake,
 * lights, reverse, turn signals), MCU version and master volume, pushed by the service callbacks.
 */
class LibreHuSource(
    private val context: Context,
) : VehicleSource {
    override val name = "LibreHU-service"

    @Volatile
    private var service: ILibreHuService? = null
    private var bound = false
    private val main = Handler(Looper.getMainLooper())
    private var onData: (VehicleData) -> Unit = {}

    private val callback =
        object : ILibreHuCallback.Stub() {
            override fun onVehicleFlags(flags: Int) = publish()

            override fun onAudioChanged() = publish()

            override fun onMcuFrame(
                cmd: Int,
                data: ByteArray?,
                fromMcu: Boolean,
            ) {}

            override fun onKey(
                channel: Int,
                values: IntArray?,
                released: Boolean,
                learning: Boolean,
            ) {}

            override fun onCanData(data: ByteArray?) {}
        }

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                val s = binder?.let { ILibreHuService.Stub.asInterface(it) } ?: return
                service = s
                try {
                    s.registerCallback(callback)
                } catch (e: RemoteException) {
                    Log.w(TAG, "LibreHU-service: ${e.message}")
                }
                publish()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                main.post { onData(VehicleData(linkOnline = false)) }
            }
        }

    override fun start(onData: (VehicleData) -> Unit) {
        this.onData = onData
        bound =
            try {
                context.bindService(Intent(ACTION_BIND).setPackage(PACKAGE), connection, Context.BIND_AUTO_CREATE)
            } catch (e: SecurityException) {
                Log.w(TAG, "LibreHU-service: ${e.message}")
                false
            }
        if (!bound) onData(VehicleData(linkOnline = false))
    }

    override fun stop() {
        try {
            service?.unregisterCallback(callback)
        } catch (_: RemoteException) {
        }
        if (bound) {
            try {
                context.unbindService(connection)
            } catch (_: IllegalArgumentException) {
            }
        }
        bound = false
        service = null
    }

    private fun publish() {
        val s = service ?: return
        val d =
            try {
                val f = s.vehicleFlags
                val online = f and FLAG_MCU_ONLINE != 0

                // Without the MCU link the flags mean nothing: report them as unknown.
                fun flag(bit: Int): Boolean? = if (online) f and bit != 0 else null
                VehicleData(
                    linkOnline = online,
                    acc = flag(FLAG_ACC),
                    handbrake = flag(FLAG_HANDBRAKE),
                    headlights = flag(FLAG_HEADLIGHT),
                    reverse = flag(FLAG_REVERSE),
                    turnLeft = flag(FLAG_TURN_LEFT),
                    turnRight = flag(FLAG_TURN_RIGHT),
                    mcuVersion = s.mcuVersion,
                    volume = s.volume,
                    maxVolume = s.maxVolume,
                )
            } catch (e: RemoteException) {
                VehicleData(linkOnline = false)
            }
        main.post { onData(d) }
    }

    private companion object {
        const val TAG = "LibreHU-Widgets"
        const val PACKAGE = "org.librehu.service"
        const val ACTION_BIND = "org.librehu.service.BIND"

        // org.librehu.service.LibreHu flags.
        const val FLAG_MCU_ONLINE = 1 shl 0
        const val FLAG_ACC = 1 shl 1
        const val FLAG_HANDBRAKE = 1 shl 2
        const val FLAG_HEADLIGHT = 1 shl 3
        const val FLAG_REVERSE = 1 shl 4
        const val FLAG_TURN_LEFT = 1 shl 5
        const val FLAG_TURN_RIGHT = 1 shl 6
    }
}
