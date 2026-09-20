package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.DoseHistory
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "medvault_reminders_channel"
        const val CHANNEL_NAME = "Medication Reminders"
        const val ACTION_MARK_TAKEN = "com.example.medvault.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.example.medvault.ACTION_SNOOZE"
        const val ACTION_SKIP = "com.example.medvault.ACTION_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medication"
        val dose = intent.getStringExtra(AlarmScheduler.EXTRA_DOSE) ?: "1 Dose"
        val beforeFood = intent.getBooleanExtra(AlarmScheduler.EXTRA_BEFORE_FOOD, false)
        val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = reminderId.toInt()

        when (intent.action) {
            AlarmScheduler.ACTION_REMINDER -> {
                showReminderNotification(
                    context = context,
                    notificationManager = notificationManager,
                    notificationId = notificationId,
                    reminderId = reminderId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dose = dose,
                    beforeFood = beforeFood,
                    scheduledTime = scheduledTime
                )
            }

            ACTION_MARK_TAKEN -> {
                notificationManager.cancel(notificationId)
                val database = AppDatabase.getInstance(context)
                CoroutineScope(Dispatchers.IO).launch {
                    database.doseHistoryDao().insertDoseHistory(
                        DoseHistory(
                            reminderId = reminderId,
                            medicineId = medicineId,
                            scheduledTime = scheduledTime,
                            dateTime = System.currentTimeMillis(),
                            status = "TAKEN"
                        )
                    )
                    database.medicineDao().decrementStock(medicineId, 1)

                    // Check low stock
                    val med = database.medicineDao().getMedicineById(medicineId)
                    if (med != null && med.stockQuantity <= med.lowStockThreshold) {
                        showLowStockNotification(context, notificationManager, med.brandName, med.stockQuantity)
                    }
                }
            }

            ACTION_SNOOZE -> {
                notificationManager.cancel(notificationId)
                AlarmScheduler.scheduleSnooze(
                    context = context,
                    reminderId = reminderId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dose = dose,
                    beforeFood = beforeFood,
                    snoozeDurationMillis = 10 * 60 * 1000L
                )
            }

            ACTION_SKIP -> {
                notificationManager.cancel(notificationId)
                val database = AppDatabase.getInstance(context)
                CoroutineScope(Dispatchers.IO).launch {
                    database.doseHistoryDao().insertDoseHistory(
                        DoseHistory(
                            reminderId = reminderId,
                            medicineId = medicineId,
                            scheduledTime = scheduledTime,
                            dateTime = System.currentTimeMillis(),
                            status = "SKIPPED"
                        )
                    )
                }
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        reminderId: Long,
        medicineId: Long,
        medicineName: String,
        dose: String,
        beforeFood: Boolean,
        scheduledTime: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take your scheduled medications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap opens app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Taken
        val takenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSE, dose)
            putExtra(AlarmScheduler.EXTRA_BEFORE_FOOD, beforeFood)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Skip
        val skipIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val foodNote = if (beforeFood) "Before food" else "After food"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for: $medicineName")
            .setContentText("Dose: $dose ($foodNote)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Time to take your medication: $medicineName\nDosage: $dose ($foodNote)"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Taken", takenPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze (10m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Skip", skipPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showLowStockNotification(
        context: Context,
        notificationManager: NotificationManager,
        medicineName: String,
        remainingStock: Int
    ) {
        val lowStockNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Low Stock Alert: $medicineName")
            .setContentText("Only $remainingStock remaining! Remember to restock soon.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt() + 50000, lowStockNotification)
    }
}
