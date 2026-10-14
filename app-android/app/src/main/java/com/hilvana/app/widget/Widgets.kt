package com.hilvana.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Redibuja los widgets con datos tras un cambio (guardar, completar, borrar). */
suspend fun refreshWidgets(context: Context) {
    runCatching { MyDayWidget().updateAll(context) }
    runCatching { FocusWidget().updateAll(context) }
}
