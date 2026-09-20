package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.MedicationRepository
import com.example.receiver.MedicationReminderReceiver

class MedVaultApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MedicationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = MedicationRepository(
            medicineDao = database.medicineDao(),
            reminderDao = database.reminderDao(),
            doseHistoryDao = database.doseHistoryDao()
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MedicationReminderReceiver.CHANNEL_ID,
                MedicationReminderReceiver.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medication reminder alerts"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
