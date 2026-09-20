package com.example.data.model

data class TodayDoseItem(
    val reminderId: Long,
    val medicineId: Long,
    val medicineBrandName: String,
    val medicineGenericName: String,
    val strength: String,
    val dosageForm: String,
    val dose: String,
    val scheduledTimeMillis: Long,
    val timeFormatted: String, // e.g. "08:00 AM"
    val beforeFood: Boolean,
    val status: String, // PENDING, TAKEN, MISSED, SKIPPED
    val historyId: Long? = null,
    val frontImagePath: String? = null,
    val remainingStock: Int = 0,
    val lowStock: Boolean = false
)

data class VisualMatchResult(
    val medicine: Medicine,
    val matchingImage: MedicineImage,
    val similarityPercentage: Float
)
