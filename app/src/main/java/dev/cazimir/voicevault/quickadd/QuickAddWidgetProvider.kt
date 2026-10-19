package dev.cazimir.voicevault.quickadd

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.cazimir.voicevault.R

/** Single-tap launcher for [QuickAddActivity] - the widget has no state of its own. */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, QuickAddActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
                setOnClickPendingIntent(R.id.quick_add_widget_icon, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
