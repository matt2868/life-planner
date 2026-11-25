package io.github.matt2868.lifeplanner

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Daily reminder: an alarm at the chosen time checks whether anything was filled in today
 * and, if not, posts a notification. The page keeps "last filled" up to date.
 */
object Reminders {
    const val ACTION_REMIND = "io.github.matt2868.lifeplanner.REMIND"
    private const val PREFS = "reminder"
    private const val CHANNEL = "daily_reminder"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setConfig(context: Context, json: String) {
        val o = try {
            JSONObject(json)
        } catch (e: Exception) {
            return
        }
        val parts = o.optString("time", "20:00").split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        prefs(context).edit()
            .putBoolean("on", o.optBoolean("on"))
            .putInt("hour", hour)
            .putInt("minute", minute)
            .apply()
        schedule(context)
    }

    fun setLastFilled(context: Context, date: String) {
        prefs(context).edit().putString("last", date).apply()
    }

    fun markFilledToday(context: Context) = setLastFilled(context, LocalDate.now().toString())

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        10,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        val p = prefs(context)
        if (!p.getBoolean("on", false)) return
        val now = ZonedDateTime.now()
        var at = now.withHour(p.getInt("hour", 20)).withMinute(p.getInt("minute", 0)).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending)
    }

    fun remindIfEmpty(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean("on", false)) return
        if (p.getString("last", "") == LocalDate.now().toString()) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val channel = NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "A nudge when nothing has been filled in today"
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        val open = PendingIntent.getActivity(
            context,
            11,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Fill in your planner")
            .setContentText("Nothing logged today yet.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(1, notification)
        } catch (e: SecurityException) {
        }
    }
}
