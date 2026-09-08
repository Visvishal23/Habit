package com.habitflow.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitflow.app.HabitFlowApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Re-schedules all enabled reminders after a device restart (alarms don't survive a reboot). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as HabitFlowApplication
        val pendingResult = goAsync()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                app.reminderRepository.getAllEnabled().forEach { reminder ->
                    ReminderScheduler.schedule(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
