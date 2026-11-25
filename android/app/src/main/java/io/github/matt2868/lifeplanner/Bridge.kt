package io.github.matt2868.lifeplanner

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat
import java.lang.ref.WeakReference

/** Methods the planner page can call as window.PlannerApp. */
class Bridge(private val context: Context, activity: MainActivity) {
    private val activityRef = WeakReference(activity)

    @JavascriptInterface
    fun setWidgetData(json: String) {
        WidgetStore.setData(context, json)
        BaseTodoWidget.refreshAll(context)
    }

    @JavascriptInterface
    fun takeWidgetActions(): String = WidgetStore.takeActions(context)

    @JavascriptInterface
    fun setReminder(json: String) {
        Reminders.setConfig(context, json)
    }

    @JavascriptInterface
    fun setFilled(date: String) {
        Reminders.setLastFilled(context, date)
    }

    @JavascriptInterface
    fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @JavascriptInterface
    fun setChrome(dark: Boolean, color: Int) {
        activityRef.get()?.setChrome(dark, color)
    }

    @JavascriptInterface
    fun requestNotifications() {
        activityRef.get()?.requestNotificationPermission()
    }
}
