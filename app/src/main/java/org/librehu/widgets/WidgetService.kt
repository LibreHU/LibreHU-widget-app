package org.librehu.widgets

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.librehu.widgets.data.SettingsStore
import org.librehu.widgets.data.ThemeFollower
import org.librehu.widgets.data.ThemeMode
import org.librehu.widgets.data.VehicleData
import org.librehu.widgets.data.WidgetSettings
import org.librehu.widgets.source.DemoSource
import org.librehu.widgets.source.VehicleSource
import org.librehu.widgets.widget.WidgetRenderer

/**
 * Keeps the widgets up to date: reads the vehicle source (head unit or demo), computes the theme and redraws
 * every widget on changes. Runs in the foreground while widgets exist (Android 8+ background limits).
 */
class WidgetService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settings: SettingsStore
    private var source: VehicleSource? = null
    private var follower: ThemeFollower? = null
    private val followed = MutableStateFlow(true to 0)

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore.get(this)
        startForegroundCompat()
        follower = ThemeFollower(this) { dark, accent -> followed.value = dark to accent }.also { it.start() }

        // Source: demo or the head unit, switched when the setting changes.
        scope.launch {
            settings.settings.map { it.demo }.distinctUntilChanged().collect { demo ->
                source?.stop()
                val s = if (demo) DemoSource() else VehicleSource.create(this@WidgetService)
                _sourceName.value = s.name
                source = s
                s.start(::onData)
            }
        }
        // Effective theme.
        scope.launch {
            combine(settings.settings, followed, _data) { s, f, d ->
                when (s.theme) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.HEADLIGHTS -> d.headlights ?: f.first
                    ThemeMode.FOLLOW -> f.first
                }
            }.distinctUntilChanged().collect { _dark.value = it }
        }
        scope.launch {
            followed.collect { _accent.value = it.second }
        }
        // Redraw on any change, and every 30 s for the trip time.
        scope.launch {
            combine(_data, settings.settings, _dark, _accent) { d, s, dark, accent -> listOf(d, s, dark, accent) }.collect { v ->
                WidgetRenderer.updateAll(this@WidgetService, v[0] as VehicleData, v[1] as WidgetSettings, v[2] as Boolean, v[3] as Int)
            }
        }
        scope.launch {
            while (true) {
                delay(30_000)
                WidgetRenderer.updateAll(this@WidgetService, _data.value, settings.settings.value, _dark.value, _accent.value)
            }
        }
    }

    /** Adds the trip start when the source does not provide it. */
    private fun onData(d: VehicleData) {
        val previous = _data.value
        val since =
            when {
                d.accSince != null -> d.accSince

                d.acc != true -> null

                previous.acc == true && previous.accSince != null -> previous.accSince

                // Ignition already on when we start: the SoC boots with the ignition, use the boot time.
                previous.acc == null -> 0L

                else -> SystemClock.elapsedRealtime()
            }
        _data.value = d.copy(accSince = since)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_REFRESH) {
            WidgetRenderer.updateAll(this, _data.value, settings.settings.value, _dark.value, _accent.value)
        }
        if (!WidgetRenderer.hasWidgets(this) && intent?.action == ACTION_STOP_IF_UNUSED) stopSelf()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        source?.stop()
        follower?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val n =
            Notification
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_probe_link)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notif_text))
                .setContentIntent(
                    PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
                ).setOngoing(true)
                .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    companion object {
        private const val CHANNEL_ID = "widgets"
        private const val NOTIFICATION_ID = 1
        const val ACTION_REFRESH = "org.librehu.widgets.REFRESH"
        const val ACTION_STOP_IF_UNUSED = "org.librehu.widgets.STOP_IF_UNUSED"

        private val _data = MutableStateFlow(VehicleData())
        private val _dark = MutableStateFlow(true)
        private val _accent = MutableStateFlow(0)
        private val _sourceName = MutableStateFlow("")

        /** Shared with the settings app (same process). */
        val data: StateFlow<VehicleData> = _data.asStateFlow()
        val dark: StateFlow<Boolean> = _dark.asStateFlow()

        /** Accent of LibreHU Launcher (ARGB, 0 = none). */
        val accent: StateFlow<Int> = _accent.asStateFlow()
        val sourceName: StateFlow<String> = _sourceName.asStateFlow()

        fun start(
            context: Context,
            action: String = ACTION_REFRESH,
        ) {
            try {
                context.startForegroundService(Intent(context, WidgetService::class.java).setAction(action))
            } catch (_: Exception) {
            }
        }
    }
}
