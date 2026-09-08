package com.habitflow.app.data.repository

import com.habitflow.app.data.local.dao.ReminderDao
import com.habitflow.app.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    fun observeForHabit(habitId: Long): Flow<List<Reminder>> = reminderDao.observeForHabit(habitId)
    suspend fun getAllEnabled(): List<Reminder> = reminderDao.getAllEnabled()
    suspend fun add(reminder: Reminder): Long = reminderDao.insert(reminder)
    suspend fun update(reminder: Reminder) = reminderDao.update(reminder)
    suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
}
