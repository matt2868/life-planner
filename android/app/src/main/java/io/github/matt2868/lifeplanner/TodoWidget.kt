package io.github.matt2868.lifeplanner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews

/** One provider per list, so each can be placed (or stacked) on its own. */
class TodoWidget : BaseTodoWidget("all")
class TodayWidget : BaseTodoWidget("day")
class WorkWidget : BaseTodoWidget("work")
class GeneralWidget : BaseTodoWidget("gen")

/** Home-screen to-do widget. Mode "all" shows today, work and general together. */
abstract class BaseTodoWidget(private val mode: String) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, manager, ids, mode, this::class.java)
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

        private val providers: List<Pair<Class<out AppWidgetProvider>, String>> = listOf(
            TodoWidget::class.java to "all",
            TodayWidget::class.java to "day",
            WorkWidget::class.java to "work",
            GeneralWidget::class.java to "gen"
        )

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            for ((cls, mode) in providers) {
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) update(context, manager, ids, mode, cls)
            }
        }

        @Suppress("DEPRECATION")
        private fun update(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            mode: String,
            cls: Class<out AppWidgetProvider>
        ) {
            for (id in ids) manager.updateAppWidget(id, build(context, id, mode, cls))
            manager.notifyAppWidgetViewDataChanged(ids, R.id.list)
        }

        @Suppress("DEPRECATION")
        private fun build(
            context: Context,
            widgetId: Int,
            mode: String,
            cls: Class<out AppWidgetProvider>
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget)
            val look = WidgetLook.load(context)
            look.tint(views, R.id.bg) { it.bg }
            look.tint(views, R.id.add_bg) { it.acc }
            look.tint(views, R.id.add_icon) { it.accInk }
            look.text(views, R.id.sub) { it.ink2 }
            look.text(views, R.id.empty) { it.ink2 }

            val title = when (mode) {
                "work" -> context.getString(R.string.widget_title_work)
                "gen" -> context.getString(R.string.widget_title_general)
                else -> context.getString(R.string.widget_title_today)
            }
            val art = TitleArt.bitmap(context, title)
            if (art != null) {
                views.setImageViewBitmap(R.id.title_img, art)
                views.setContentDescription(R.id.title_img, title)
                views.setViewVisibility(R.id.title_img, View.VISIBLE)
                views.setViewVisibility(R.id.title, View.GONE)
                look.tint(views, R.id.title_img) { it.ink }
            } else {
                views.setTextViewText(R.id.title, title)
                look.text(views, R.id.title) { it.ink }
            }
            val sub = WidgetStore.summary(context, mode)
            views.setTextViewText(R.id.sub, sub)
            views.setViewVisibility(R.id.sub, if (sub.isEmpty()) View.GONE else View.VISIBLE)
            views.setTextViewText(
                R.id.empty,
                if (WidgetStore.isSetup(context)) context.getString(R.string.widget_setup) else ""
            )

            val service = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("mode", mode)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.list, service)
            views.setEmptyView(R.id.list, R.id.empty)

            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val toggle = Intent(context, cls).setAction(ACTION_TOGGLE)
            views.setPendingIntentTemplate(
                R.id.list,
                PendingIntent.getBroadcast(context, 0, toggle, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
            )

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val addAction = when (mode) {
                "work" -> "add-work"
                "gen" -> "add-gen"
                else -> "add"
            }
            val add = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_ACTION, addAction)
            val code = providers.indexOfFirst { it.second == mode } * 10
            views.setOnClickPendingIntent(R.id.head, PendingIntent.getActivity(context, 1 + code, open, flags))
            views.setOnClickPendingIntent(R.id.empty, PendingIntent.getActivity(context, 3 + code, open, flags))
            views.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(context, 2 + code, add, flags))
            return views
        }
    }
}
