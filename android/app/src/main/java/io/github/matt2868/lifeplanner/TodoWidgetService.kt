package io.github.matt2868.lifeplanner

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.content.ContextCompat

/** Supplies the rows of the widget's list. */
class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var rows: List<Row> = emptyList()

        override fun onCreate() {}
        override fun onDataSetChanged() {
            rows = WidgetStore.rows(context)
        }
        override fun onDestroy() {}
        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val row = rows.getOrNull(position)
                ?: return RemoteViews(context.packageName, R.layout.widget_header)
            if (row.type == WidgetStore.HEADER) {
                return RemoteViews(context.packageName, R.layout.widget_header).apply {
                    setTextViewText(R.id.text, row.text)
                }
            }
            val views = RemoteViews(context.packageName, R.layout.widget_item)
            val text: CharSequence = if (row.done) {
                SpannableString(row.text).apply {
                    setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else row.text
            views.setTextViewText(R.id.text, text)
            views.setTextColor(
                R.id.text,
                ContextCompat.getColor(context, if (row.done) R.color.w_ink2 else R.color.w_ink)
            )
            views.setImageViewResource(R.id.check, if (row.done) R.drawable.ic_check_on else R.drawable.ic_check_off)
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
