package io.github.matt2868.lifeplanner

import android.content.Context
import android.webkit.JavascriptInterface

/** Methods the planner page can call as window.PlannerApp. */
class Bridge(private val context: Context) {
    @JavascriptInterface
    fun setWidgetData(json: String) {
        WidgetStore.setData(context, json)
        TodoWidget.refreshAll(context)
    }

    @JavascriptInterface
    fun takeWidgetActions(): String = WidgetStore.takeActions(context)
}
