package com.habitflow.app.data.local.dao

import androidx.room.*
import com.habitflow.app.data.local.entity.JournalEntry
import com.habitflow.app.data.local.entity.JournalPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE habitId = :habitId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeForHabit(habitId: Long): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries")
    suspend fun getAllOnce(): List<JournalEntry>

    @Insert
    suspend fun insertEntry(entry: JournalEntry): Long

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_photos WHERE journalEntryId = :entryId")
    fun observePhotosForEntry(entryId: Long): Flow<List<JournalPhoto>>

    @Query("SELECT * FROM journal_photos")
    suspend fun getAllPhotosOnce(): List<JournalPhoto>

    @Insert
    suspend fun insertPhoto(photo: JournalPhoto): Long

    @Delete
    suspend fun deletePhoto(photo: JournalPhoto)
}
