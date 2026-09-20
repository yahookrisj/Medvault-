package com.example.data.repository

import com.example.data.local.DoseHistoryDao
import com.example.data.local.MedicineDao
import com.example.data.local.ReminderDao
import com.example.data.model.DoseHistory
import com.example.data.model.Medicine
import com.example.data.model.MedicineImage
import com.example.data.model.MedicineWithImages
import com.example.data.model.Reminder
import com.example.data.model.TodayDoseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MedicationRepository(
    private val medicineDao: MedicineDao,
    private val reminderDao: ReminderDao,
    private val doseHistoryDao: DoseHistoryDao
) {

    val allMedicinesWithImages: Flow<List<MedicineWithImages>> =
        medicineDao.getAllMedicinesWithImages()

    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveReminders()

    val allDoseHistory: Flow<List<DoseHistory>> = doseHistoryDao.getAllHistory()

    fun getMedicineWithImages(id: Long): Flow<MedicineWithImages?> =
        medicineDao.getMedicineWithImages(id)

    fun getRemindersForMedicine(medicineId: Long): Flow<List<Reminder>> =
        reminderDao.getRemindersForMedicine(medicineId)

    fun getHistoryForMedicine(medicineId: Long): Flow<List<DoseHistory>> =
        doseHistoryDao.getHistoryForMedicine(medicineId)

    suspend fun getMedicineById(id: Long): Medicine? = medicineDao.getMedicineById(id)

    suspend fun insertMedicine(medicine: Medicine): Long =
        medicineDao.insertMedicine(medicine)

    suspend fun updateMedicine(medicine: Medicine) =
        medicineDao.updateMedicine(medicine)

    suspend fun deleteMedicine(medicineWithImages: MedicineWithImages) {
        // Delete image files from disk
        medicineWithImages.images.forEach { image ->
            try {
                val file = File(image.imagePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        medicineDao.deleteMedicine(medicineWithImages.medicine)
    }

    suspend fun insertImage(image: MedicineImage): Long =
        medicineDao.insertImage(image)

    suspend fun updateImageFeatureVector(imageId: Long, vector: String) =
        medicineDao.updateImageFeatureVector(imageId, vector)

    suspend fun deleteImage(image: MedicineImage) {
        try {
            val file = File(image.imagePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
        medicineDao.deleteImage(image)
    }

    suspend fun getAllImages(): List<MedicineImage> = medicineDao.getAllImages()

    suspend fun updateStock(medicineId: Long, newStock: Int) =
        medicineDao.updateStock(medicineId, newStock)

    suspend fun decrementStock(medicineId: Long, amount: Int = 1) =
        medicineDao.decrementStock(medicineId, amount)

    suspend fun insertReminder(reminder: Reminder): Long =
        reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) =
        reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) =
        reminderDao.deleteReminder(reminder)

    suspend fun recordDoseStatus(
        reminderId: Long,
        medicineId: Long,
        scheduledTime: Long,
        status: String, // TAKEN, MISSED, SKIPPED
        notes: String? = null
    ) {
        val existing = doseHistoryDao.getHistoryByReminderAndSchedule(reminderId, scheduledTime)
        if (existing != null) {
            doseHistoryDao.insertDoseHistory(
                existing.copy(status = status, dateTime = System.currentTimeMillis(), notes = notes)
            )
        } else {
            doseHistoryDao.insertDoseHistory(
                DoseHistory(
                    reminderId = reminderId,
                    medicineId = medicineId,
                    scheduledTime = scheduledTime,
                    dateTime = System.currentTimeMillis(),
                    status = status,
                    notes = notes
                )
            )
        }

        // Automatically reduce stock when taken!
        if (status == "TAKEN") {
            decrementStock(medicineId, 1)
        }
    }

    /**
     * Compute scheduled doses for a given target calendar date, combined with adherence status
     */
    fun getTodayDoseItems(calendar: Calendar): Flow<List<TodayDoseItem>> {
        val startOfDay = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val historyFlow = doseHistoryDao.getHistoryBetween(startOfDay, endOfDay)

        return combine(
            allMedicinesWithImages,
            activeReminders,
            historyFlow
        ) { medicinesList, remindersList, historyList ->
            val medicineMap = medicinesList.associateBy { it.medicine.id }
            val historyMap = historyList.associateBy { "${it.reminderId}_${it.scheduledTime}" }

            val items = mutableListOf<TodayDoseItem>()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ...
            val targetDateMillis = calendar.timeInMillis
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            for (reminder in remindersList) {
                val medWithImages = medicineMap[reminder.medicineId] ?: continue
                val medicine = medWithImages.medicine

                // Check start and end date
                if (targetDateMillis < reminder.startDate) continue
                if (reminder.endDate != null && targetDateMillis > reminder.endDate) continue

                // Check schedule frequency
                val isScheduledToday = when (reminder.scheduleType) {
                    "WEEKLY" -> {
                        val days = reminder.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                        days == null || days.contains(dayOfWeek)
                    }
                    else -> true
                }

                if (!isScheduledToday) continue

                // Parse reminder times (e.g. "08:00" or "08:00,14:00,20:00")
                val times = reminder.reminderTime.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (timeStr in times) {
                    val parts = timeStr.split(":")
                    if (parts.size != 2) continue
                    val hour = parts[0].toIntOrNull() ?: continue
                    val minute = parts[1].toIntOrNull() ?: continue

                    val scheduledCal = (calendar.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val scheduledTimeMillis = scheduledCal.timeInMillis

                    val history = historyMap["${reminder.id}_$scheduledTimeMillis"]
                    val currentStatus = when {
                        history != null -> history.status
                        scheduledTimeMillis < System.currentTimeMillis() - 3600000L -> "MISSED"
                        else -> "PENDING"
                    }

                    items.add(
                        TodayDoseItem(
                            reminderId = reminder.id,
                            medicineId = medicine.id,
                            medicineBrandName = medicine.brandName,
                            medicineGenericName = medicine.genericName,
                            strength = medicine.strength,
                            dosageForm = medicine.dosageForm,
                            dose = reminder.dose,
                            scheduledTimeMillis = scheduledTimeMillis,
                            timeFormatted = timeFormat.format(scheduledCal.time),
                            beforeFood = reminder.beforeFood,
                            status = currentStatus,
                            historyId = history?.id,
                            frontImagePath = medWithImages.frontImage?.imagePath,
                            remainingStock = medicine.stockQuantity,
                            lowStock = medicine.stockQuantity <= medicine.lowStockThreshold
                        )
                    )
                }
            }

            items.sortedBy { it.scheduledTimeMillis }
        }
    }
}
