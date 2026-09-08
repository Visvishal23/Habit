package com.habitflow.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitflow.app.data.local.entity.Reminder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules local, exact alarms via AlarmManager - no server, no push
 * service, no network of any kind is involved.
 */
object ReminderScheduler {

    private const val EXTRA_REMINDER_ID = "reminder_id"
    private const val EXTRA_LABEL = "label"

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.isEnabled) {
            cancel(context, reminder.id)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTrigger = nextTriggerMillis(reminder)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_LABEL, reminder.label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /** Finds the next epoch millis this reminder should fire, given its weekday mask. */
    private fun nextTriggerMillis(reminder: Reminder): Long {
        val zone = ZoneId.systemDefault()
        val allowedDays = reminder.weekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        var date = LocalDate.now(zone)
        val time = LocalTime.of(reminder.hour, reminder.minute)

        repeat(8) { offset ->
            val candidateDate = date.plusDays(offset.toLong())
            val isoDay = candidateDate.dayOfWeek.value
            if (allowedDays.contains(isoDay) || allowedDays.isEmpty()) {
                val candidateDateTime = candidateDate.atTime(time)
                val candidateMillis = candidateDateTime.atZone(zone).toInstant().toEpochMilli()
                if (candidateMillis > System.currentTimeMillis()) {
                    return candidateMillis
                }
            }
        }
        // Fallback: tomorrow at the same time
        return date.plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    const val KEY_REMINDER_ID = EXTRA_REMINDER_ID
    const val KEY_LABEL = EXTRA_LABEL
}
