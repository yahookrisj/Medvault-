package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val dose: String, // e.g. "1 Tablet", "5 ml"
    val scheduleType: String, // ONCE_DAILY, TWICE_DAILY, THREE_TIMES_DAILY, EVERY_X_HOURS, WEEKLY, CUSTOM
    val reminderTime: String, // Formatted time string e.g. "08:00" or comma-separated "08:00,20:00"
    val beforeFood: Boolean = false, // true = Before Food, false = After Food
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val intervalHours: Int? = null,
    val daysOfWeek: String? = null, // e.g. "1,3,5" for Mon, Wed, Fri
    val isActive: Boolean = true
)
