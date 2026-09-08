package com.habitflow.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.habitflow.app.data.local.AppDatabase
import com.habitflow.app.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local-only backup/restore. Everything (habits, completions, journal text,
 * reminders, settings) is serialized to a single JSON file the user saves
 * with Android's normal file picker (Storage Access Framework) - nothing is
 * ever uploaded anywhere. Journal photos are embedded as base64 so the
 * result is one self-contained, easy-to-move file.
 */
class BackupRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    companion object {
        const val BACKUP_VERSION = 1
    }

    suspend fun exportToUri(uri: Uri): Result<Unit> = runCatching {
        val root = JSONObject()
        root.put("backupVersion", BACKUP_VERSION)
        root.put("exportedAtEpochMillis", System.currentTimeMillis())

        val habitDao = database.habitDao()
        val completionDao = database.habitCompletionDao()
        val journalDao = database.journalDao()
        val reminderDao = database.reminderDao()

        val habits = habitDao.getAllHabitsOnce()
        val completions = completionDao.getAllOnce()
        val entries = journalDao.getAllOnce()
        val photos = journalDao.getAllPhotosOnce()
        val reminders = reminderDao.getAllOnce()

        root.put("habits", JSONArray(habits.map { habitToJson(it) }))
        root.put("completions", JSONArray(completions.map { completionToJson(it) }))
        root.put("journalEntries", JSONArray(entries.map { entryToJson(it) }))
        root.put("journalPhotos", JSONArray(photos.mapNotNull { photoToJson(it) }))
        root.put("reminders", JSONArray(reminders.map { reminderToJson(it) }))

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString().toByteArray(Charsets.UTF_8))
        } ?: error("Could not open the selected file for writing")
    }

    suspend fun importFromUri(uri: Uri, mergeStrategy: MergeStrategy = MergeStrategy.SKIP_DUPLICATES): Result<ImportSummary> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Could not read the selected file")

        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            error("This file doesn't look like a valid HabitFlow backup")
        }

        val habitDao = database.habitDao()
        val completionDao = database.habitCompletionDao()
        val journalDao = database.journalDao()
        val reminderDao = database.reminderDao()

        val existingHabits = habitDao.getAllHabitsOnce()
        val existingNames = existingHabits.map { it.name.trim().lowercase() }.toMutableSet()

        val habitIdRemap = HashMap<Long, Long>() // backup id -> new local id
        var habitsImported = 0

        val habitsJson = root.optJSONArray("habits") ?: JSONArray()
        for (i in 0 until habitsJson.length()) {
            val obj = habitsJson.getJSONObject(i)
            val oldId = obj.getLong("id")
            val habit = jsonToHabit(obj)

            if (mergeStrategy == MergeStrategy.SKIP_DUPLICATES && existingNames.contains(habit.name.trim().lowercase())) {
                // Map to the existing habit with the same name so its history still imports sensibly
                val match = existingHabits.first { it.name.trim().lowercase() == habit.name.trim().lowercase() }
                habitIdRemap[oldId] = match.id
                continue
            }
            val newId = habitDao.insert(habit.copy(id = 0))
            habitIdRemap[oldId] = newId
            existingNames.add(habit.name.trim().lowercase())
            habitsImported++
        }

        var completionsImported = 0
        val completionsJson = root.optJSONArray("completions") ?: JSONArray()
        for (i in 0 until completionsJson.length()) {
            val obj = completionsJson.getJSONObject(i)
            val oldHabitId = obj.getLong("habitId")
            val newHabitId = habitIdRemap[oldHabitId] ?: continue
            val completion = jsonToCompletion(obj, newHabitId)
            val existing = completionDao.getForHabitOnDay(newHabitId, completion.dateEpochDay)
            if (existing == null) {
                completionDao.upsert(completion.copy(id = 0))
                completionsImported++
            }
        }

        val entryIdRemap = HashMap<Long, Long>()
        var entriesImported = 0
        val entriesJson = root.optJSONArray("journalEntries") ?: JSONArray()
        for (i in 0 until entriesJson.length()) {
            val obj = entriesJson.getJSONObject(i)
            val oldId = obj.getLong("id")
            val oldHabitId = obj.getLong("habitId")
            val newHabitId = habitIdRemap[oldHabitId] ?: continue
            val entry = jsonToEntry(obj, newHabitId)
            val newId = journalDao.insertEntry(entry.copy(id = 0))
            entryIdRemap[oldId] = newId
            entriesImported++
        }

        var photosImported = 0
        val photosJson = root.optJSONArray("journalPhotos") ?: JSONArray()
        val photosDir = File(context.filesDir, "journal_photos").apply { mkdirs() }
        for (i in 0 until photosJson.length()) {
            val obj = photosJson.getJSONObject(i)
            val oldEntryId = obj.getLong("journalEntryId")
            val newEntryId = entryIdRemap[oldEntryId] ?: continue
            val base64 = obj.optString("data", "")
            if (base64.isEmpty()) continue
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val file = File(photosDir, "${java.util.UUID.randomUUID()}.jpg")
            file.writeBytes(bytes)
            journalDao.insertPhoto(JournalPhoto(journalEntryId = newEntryId, localFilePath = file.absolutePath))
            photosImported++
        }

        var remindersImported = 0
        val remindersJson = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until remindersJson.length()) {
            val obj = remindersJson.getJSONObject(i)
            val oldHabitId = obj.getLong("habitId")
            val newHabitId = habitIdRemap[oldHabitId] ?: continue
            reminderDao.insert(jsonToReminder(obj, newHabitId).copy(id = 0))
            remindersImported++
        }

        ImportSummary(habitsImported, completionsImported, entriesImported, photosImported, remindersImported)
    }

    // ---- JSON <-> entity mapping ----

    private fun habitToJson(h: Habit) = JSONObject().apply {
        put("id", h.id); put("name", h.name); put("description", h.description)
        put("icon", h.icon); put("colorHex", h.colorHex); put("category", h.category)
        put("frequencyType", h.frequencyType.name); put("scheduledWeekdays", h.scheduledWeekdays)
        put("timesPerWeek", h.timesPerWeek); put("goalType", h.goalType.name)
        put("goalTarget", h.goalTarget); put("goalUnit", h.goalUnit)
        put("reminderEnabled", h.reminderEnabled); put("startDateEpochDay", h.startDateEpochDay)
        put("createdAtEpochMillis", h.createdAtEpochMillis); put("sortOrder", h.sortOrder)
        put("isArchived", h.isArchived)
    }

    private fun jsonToHabit(o: JSONObject) = Habit(
        id = o.getLong("id"), name = o.getString("name"), description = o.optString("description", ""),
        icon = o.optString("icon", "⭐"), colorHex = o.optString("colorHex", "#3D6E5C"),
        category = o.optString("category", "Other"),
        frequencyType = runCatching { FrequencyType.valueOf(o.getString("frequencyType")) }.getOrDefault(FrequencyType.DAILY),
        scheduledWeekdays = o.optString("scheduledWeekdays", "1,2,3,4,5,6,7"),
        timesPerWeek = o.optInt("timesPerWeek", 7),
        goalType = runCatching { GoalType.valueOf(o.getString("goalType")) }.getOrDefault(GoalType.SIMPLE_CHECK),
        goalTarget = o.optInt("goalTarget", 1), goalUnit = o.optString("goalUnit", ""),
        reminderEnabled = o.optBoolean("reminderEnabled", false),
        startDateEpochDay = o.getLong("startDateEpochDay"),
        createdAtEpochMillis = o.optLong("createdAtEpochMillis", System.currentTimeMillis()),
        sortOrder = o.optInt("sortOrder", 0), isArchived = o.optBoolean("isArchived", false)
    )

    private fun completionToJson(c: HabitCompletion) = JSONObject().apply {
        put("id", c.id); put("habitId", c.habitId); put("dateEpochDay", c.dateEpochDay)
        put("progressValue", c.progressValue); put("isComplete", c.isComplete)
        put("completedAtEpochMillis", c.completedAtEpochMillis)
    }

    private fun jsonToCompletion(o: JSONObject, newHabitId: Long) = HabitCompletion(
        id = o.optLong("id", 0), habitId = newHabitId, dateEpochDay = o.getLong("dateEpochDay"),
        progressValue = o.optInt("progressValue", 0), isComplete = o.optBoolean("isComplete", false),
        completedAtEpochMillis = o.optLong("completedAtEpochMillis", System.currentTimeMillis())
    )

    private fun entryToJson(e: JournalEntry) = JSONObject().apply {
        put("id", e.id); put("habitId", e.habitId); put("dateEpochDay", e.dateEpochDay)
        put("text", e.text); put("mood", e.mood.name); put("createdAtEpochMillis", e.createdAtEpochMillis)
    }

    private fun jsonToEntry(o: JSONObject, newHabitId: Long) = JournalEntry(
        id = o.optLong("id", 0), habitId = newHabitId, dateEpochDay = o.getLong("dateEpochDay"),
        text = o.optString("text", ""),
        mood = runCatching { Mood.valueOf(o.getString("mood")) }.getOrDefault(Mood.OKAY),
        createdAtEpochMillis = o.optLong("createdAtEpochMillis", System.currentTimeMillis())
    )

    private fun photoToJson(p: JournalPhoto): JSONObject? {
        val file = File(p.localFilePath)
        if (!file.exists()) return null
        val bytes = file.readBytes()
        return JSONObject().apply {
            put("journalEntryId", p.journalEntryId)
            put("data", Base64.encodeToString(bytes, Base64.DEFAULT))
        }
    }

    private fun reminderToJson(r: Reminder) = JSONObject().apply {
        put("id", r.id); put("habitId", r.habitId); put("hour", r.hour); put("minute", r.minute)
        put("weekdays", r.weekdays); put("label", r.label); put("isEnabled", r.isEnabled)
    }

    private fun jsonToReminder(o: JSONObject, newHabitId: Long) = Reminder(
        id = o.optLong("id", 0), habitId = newHabitId, hour = o.getInt("hour"), minute = o.getInt("minute"),
        weekdays = o.optString("weekdays", "1,2,3,4,5,6,7"), label = o.optString("label", ""),
        isEnabled = o.optBoolean("isEnabled", true)
    )
}

enum class MergeStrategy { SKIP_DUPLICATES, IMPORT_ALL }

data class ImportSummary(
    val habitsImported: Int,
    val completionsImported: Int,
    val entriesImported: Int,
    val photosImported: Int,
    val remindersImported: Int
)
