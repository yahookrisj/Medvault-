package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brandName: String,
    val genericName: String,
    val strength: String,
    val dosageForm: String, // Tablet, Capsule, Syrup, Injection, Ointment, Drops, Inhaler, Other
    val manufacturer: String? = null,
    val indication: String, // Purpose / reason e.g. "Pain relief", "Blood pressure"
    val notes: String? = null,
    val expiryDate: Long, // Epoch millis
    val batchNumber: String? = null,
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)
