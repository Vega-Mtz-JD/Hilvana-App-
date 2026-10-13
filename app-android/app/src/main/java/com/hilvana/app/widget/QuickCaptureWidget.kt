package com.hilvana.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hilvana.app.capture.CaptureActivity

/**
 * Widget "Captura rápida" (plan §5): una barra que abre el panel de captura.
 * Es también el plan B si el usuario no da el permiso de la burbuja (§7.5).
 */
class QuickCaptureWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { QuickCaptureContent() }
        }
    }
}

@androidx.compose.runtime.Composable
private fun QuickCaptureContent() {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 16.dp)
            .clickable(actionStartActivity(CaptureActivity.intent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🫧", style = TextStyle(fontSize = 20.sp))
        Spacer(GlanceModifier.width(10.dp))
        Text(
            "¿Qué no quieres olvidar?",
            style = TextStyle(fontSize = 15.sp, color = GlanceTheme.colors.onSurfaceVariant),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            "＋",
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary),
        )
    }
}

class QuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCaptureWidget()
}
