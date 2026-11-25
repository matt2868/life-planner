package io.github.matt2868.lifeplanner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Home-screen widget: today's to-dos and open general to-dos, tick to complete. */
class TodoWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val kind = intent.getStringExtra("kind") ?: return
            val id = intent.getStringExtra("id") ?: return
            WidgetStore.toggle(context, kind, intent.getStringExtra("date") ?: "", id)
            refreshAll(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "io.github.matt2868.lifeplanner.TOGGLE"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodoWidget::class.java))
            if (ids.isNotEmpty()) update(context, manager, ids)
        }

        @Suppress("DEPRECATION")
        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            for (id in ids) manager.updateAppWidget(id, build(context, id))
            manager.notifyAppWidgetViewDataChanged(ids, R.id.list)
        }

        @Suppress("DEPRECATION")
        private fun build(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget)
            val label = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK))
            views.setTextViewText(R.id.title, label)

            val service = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.list, service)
            views.setEmptyView(R.id.list, R.id.empty)

            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val toggle = Intent(context, TodoWidget::class.java).setAction(ACTION_TOGGLE)
            views.setPendingIntentTemplate(
                R.id.list,
                PendingIntent.getBroadcast(context, 0, toggle, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
            )

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val add = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_ACTION, "add")
            views.setOnClickPendingIntent(R.id.title, PendingIntent.getActivity(context, 1, open, flags))
            views.setOnClickPendingIntent(R.id.empty, PendingIntent.getActivity(context, 3, open, flags))
            views.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(context, 2, add, flags))
            return views
        }
    }
}
