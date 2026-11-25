package io.github.matt2868.lifeplanner

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory

/** Supplies the rows of the widget's list. */
class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext, intent.getStringExtra("mode") ?: "all")

    private class Factory(private val context: Context, private val mode: String) : RemoteViewsFactory {
        private var rows: List<Row> = emptyList()
        private var look: WidgetLook = WidgetLook.load(context)

        override fun onCreate() {}
        override fun onDataSetChanged() {
            rows = WidgetStore.rows(context, mode)
            look = WidgetLook.load(context)
        }
        override fun onDestroy() {}
        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val row = rows.getOrNull(position)
                ?: return RemoteViews(context.packageName, R.layout.widget_header)
            if (row.type == WidgetStore.HEADER) {
                return RemoteViews(context.packageName, R.layout.widget_header).apply {
                    setTextViewText(R.id.text, row.text)
                    look.text(this, R.id.text) { it.acc }
                }
            }
            val views = RemoteViews(context.packageName, R.layout.widget_item)
            val text: CharSequence = if (row.done) {
                SpannableString(row.text).apply {
                    setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else row.text
            views.setTextViewText(R.id.text, text)
            look.text(views, R.id.text) { if (row.done) it.ink2 else it.ink }
            views.setImageViewResource(R.id.box, if (row.done) R.drawable.w_box_fill else R.drawable.w_box)
            look.tint(views, R.id.box) { if (row.done) it.ink else it.ink2 }
            views.setViewVisibility(R.id.tick, if (row.done) View.VISIBLE else View.GONE)
            look.tint(views, R.id.tick) { it.bg }
            val next = rows.getOrNull(position + 1)
            views.setViewVisibility(R.id.divider, if (next == null || next.type == WidgetStore.HEADER) View.INVISIBLE else View.VISIBLE)
            look.tint(views, R.id.divider) { it.line }
            val fill = Intent().putExtra("kind", row.kind).putExtra("date", row.date).putExtra("id", row.id)
            views.setOnClickFillInIntent(R.id.row, fill)
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 2
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = false
    }
}
