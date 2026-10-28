package org.librehu.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.librehu.widgets.data.SettingsStore
import org.librehu.widgets.ui.CarTheme
import org.librehu.widgets.ui.ProbePicker

/** Shown when a single-probe widget is placed: pick the lamp or value it displays. */
class SingleConfigActivity : ComponentActivity() {
    /** A click on each tap, the sound chosen in LibreHU Launcher (see KeyClick). */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        org.librehu.widgets.ui.KeyClick.onTouch(this, ev, org.librehu.widgets.ui.RetroLook.clickSound)
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id =
            intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        WidgetService.start(this)
        setContent {
            CarTheme {
                ProbePicker { probe ->
                    SettingsStore.get(this).setSingleProbe(id, probe)
                    WidgetService.start(this)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                    finish()
                }
            }
        }
    }
}
