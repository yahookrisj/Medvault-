package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.DoseHistory
import com.example.data.model.Medicine
import com.example.data.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupHelper {

    suspend fun createBackup(context: Context): File = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val medicines = database.medicineDao().getAllImages() // We'll get all data
        // Query full list
        val medList = database.medicineDao().getAllMedicines()
        // Or get from repository/direct query
        val backupJson = JSONObject()
        backupJson.put("version", 1)
        backupJson.put("timestamp", System.currentTimeMillis())

        val backupDir = File(context.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }
        val file = File(backupDir, "medvault_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { out ->
            out.write(backupJson.toString(2).toByteArray())
        }
        file
    }

    suspend fun exportDatabaseToJson(context: Context): String = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val json = JSONObject()
        json.put("app", "MedVault")
        json.put("version", 1)
        json.put("exportedAt", System.currentTimeMillis())

        // Export active reminders
        val reminders = database.reminderDao().getActiveRemindersList()
        val remindersArray = JSONArray()
        for (r in reminders) {
            val rJson = JSONObject().apply {
                put("id", r.id)
                put("medicineId", r.medicineId)
                put("dose", r.dose)
                put("scheduleType", r.scheduleType)
                put("reminderTime", r.reminderTime)
                put("beforeFood", r.beforeFood)
                put("startDate", r.startDate)
                put("endDate", r.endDate ?: JSONObject.NULL)
                put("intervalHours", r.intervalHours ?: JSONObject.NULL)
                put("daysOfWeek", r.daysOfWeek ?: JSONObject.NULL)
                put("isActive", r.isActive)
            }
            remindersArray.put(rJson)
        }
        json.put("reminders", remindersArray)

        json.toString(2)
    }

    suspend fun restoreDatabaseFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(jsonString)
            if (!json.has("app") || json.getString("app") != "MedVault") {
                return@withContext false
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
