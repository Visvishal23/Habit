package com.habitflow.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    fun today(): LocalDate = LocalDate.now()

    fun greeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    fun formatHeaderDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))

    fun monthYear(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    fun weekdayShort(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}
