package com.habitflow.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.habitflow.app.HabitFlowApplication
import com.habitflow.app.data.local.entity.Habit
import com.habitflow.app.domain.StreakCalculator
import java.time.LocalDate

private val HABIT_ID_KEY = ActionParameters.Key<Long>("habit_id")

/**
 * "Today's Habits" widget: lists today's scheduled habits with a check state,
 * reading directly from the local Room database. Entirely offline - the
 * widget never makes a network call; tapping a row just flips that habit's
 * completion for today in the same local database the app uses.
 */
class TodaysHabitsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HabitFlowApplication
        val today = LocalDate.now()

        val allHabits = app.database.habitDao().getAllHabitsOnce().filter { !it.isArchived }
        val completedIds = app.database.habitCompletionDao().getAllOnce()
            .filter { it.dateEpochDay == today.toEpochDay() && it.isComplete }
            .map { it.habitId }
            .toSet()
        val scheduledToday: List<Habit> = allHabits.filter { StreakCalculator.isScheduledOn(it, today) }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F6F5))
                    .padding(12.dp)
            ) {
                Text("Today's Habits", style = TextStyle(fontSize = 15.sp))
                if (scheduledToday.isEmpty()) {
                    Text("No habits scheduled today")
                } else {
                    scheduledToday.take(6).forEach { habit ->
                        val isDone = completedIds.contains(habit.id)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(HABIT_ID_KEY to habit.id)))
                        ) {
                            Text(if (isDone) "✓ " else "○ ")
                            Text(habit.name)
                        }
                    }
                }
            }
        }
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HABIT_ID_KEY] ?: return
        val app = context.applicationContext as HabitFlowApplication
        val habit = app.database.habitDao().getHabit(habitId) ?: return
        app.habitRepository.toggleCompletion(habit, LocalDate.now())
        TodaysHabitsWidget().updateAll(context)
    }
}
