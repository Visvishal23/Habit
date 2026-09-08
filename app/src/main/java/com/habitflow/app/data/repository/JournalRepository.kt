package com.habitflow.app.data.repository

import com.habitflow.app.data.local.dao.JournalDao
import com.habitflow.app.data.local.entity.JournalEntry
import com.habitflow.app.data.local.entity.JournalPhoto
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/** Journal photos are copied into app-private storage so they survive even
 * if the original picked file/URI later becomes unavailable, and are never
 * sent anywhere off-device. */
class JournalRepository(
    private val journalDao: JournalDao,
    private val appFilesDir: File
) {
    private val photosDir: File by lazy {
        File(appFilesDir, "journal_photos").apply { mkdirs() }
    }

    fun observeForHabit(habitId: Long): Flow<List<JournalEntry>> = journalDao.observeForHabit(habitId)
    fun observeAll(): Flow<List<JournalEntry>> = journalDao.observeAll()
    fun observePhotosForEntry(entryId: Long): Flow<List<JournalPhoto>> = journalDao.observePhotosForEntry(entryId)

    suspend fun addEntry(entry: JournalEntry): Long = journalDao.insertEntry(entry)
    suspend fun updateEntry(entry: JournalEntry) = journalDao.updateEntry(entry)
    suspend fun deleteEntry(entry: JournalEntry) = journalDao.deleteEntry(entry)

    fun copyPhotoIntoStorage(sourceBytes: ByteArray): String {
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        file.writeBytes(sourceBytes)
        return file.absolutePath
    }

    suspend fun addPhoto(entryId: Long, localPath: String) {
        journalDao.insertPhoto(JournalPhoto(journalEntryId = entryId, localFilePath = localPath))
    }

    suspend fun deletePhoto(photo: JournalPhoto) {
        runCatching { File(photo.localFilePath).delete() }
        journalDao.deletePhoto(photo)
    }
}
