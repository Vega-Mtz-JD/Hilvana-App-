package com.hilvana.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import com.hilvana.app.R
import com.hilvana.app.bubble.BubbleToggleActivity
import com.hilvana.app.data.UserPrefs

/** Widget "Burbuja": un ícono redondo que muestra u oculta la burbuja flotante. */
class BubbleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dynamicColor = UserPrefs(context).dynamicColor
        provideContent {
            HilvanaWidgetTheme(dynamicColor = dynamicColor) { BubbleContent() }
        }
    }
}

@androidx.compose.runtime.Composable
private fun BubbleContent() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(100.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(actionStartActivity(BubbleToggleActivity.intent(context))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_stat_hilvana),
            contentDescription = "Mostrar u ocultar la burbuja",
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(30.dp),
        )
    }
}

class BubbleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BubbleWidget()
}
