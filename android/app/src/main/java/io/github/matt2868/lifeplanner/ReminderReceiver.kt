package io.github.matt2868.lifeplanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires the daily reminder check, and re-arms the alarm after a restart or app update. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Reminders.ACTION_REMIND -> {
                Reminders.remindIfEmpty(context)
                Reminders.schedule(context)
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Reminders.schedule(context)
        }
    }
}
