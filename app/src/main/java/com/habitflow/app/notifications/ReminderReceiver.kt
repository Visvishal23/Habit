package com.habitflow.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitflow.app.HabitFlowApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Fires when a scheduled reminder time arrives; shows a notification and re-schedules for next week. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.KEY_REMINDER_ID, -1L)
        val label = intent.getStringExtra(ReminderScheduler.KEY_LABEL) ?: "Time for your habit"
        if (reminderId <= 0) return

        NotificationHelper.showReminder(context, reminderId, label)

        // Re-arm for the following occurrence
        val app = context.applicationContext as HabitFlowApplication
        val pendingResult = goAsync()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val reminders = app.reminderRepository.getAllEnabled()
                reminders.find { it.id == reminderId }?.let { reminder ->
                    ReminderScheduler.schedule(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
