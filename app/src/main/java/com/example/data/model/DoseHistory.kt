package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_history",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId"), Index("dateTime")]
)
data class DoseHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val medicineId: Long,
    val scheduledTime: Long, // Epoch millis when the dose was scheduled
    val dateTime: Long = System.currentTimeMillis(), // Epoch millis when recorded
    val status: String, // TAKEN, MISSED, SKIPPED
    val notes: String? = null
)
