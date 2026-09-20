package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Medicine
import com.example.data.model.Reminder
import com.example.receiver.MedicationReminderReceiver
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_REMINDER = "com.example.medvault.ACTION_MEDICATION_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSE = "extra_dose"
    const val EXTRA_BEFORE_FOOD = "extra_before_food"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"

    fun scheduleReminder(
        context: Context,
        reminder: Reminder,
        medicine: Medicine
    ) {
        if (!reminder.isActive) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Check exact alarm permission on Android 12+ (S+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Cannot schedule exact alarms; fallback to standard set
            }
        }

        val times = reminder.reminderTime.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val now = System.currentTimeMillis()

        times.forEachIndexed { index, timeStr ->
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
                val minute = parts[1].toIntOrNull() ?: return@forEachIndexed

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // If time already passed today, schedule for tomorrow
                    if (timeInMillis <= now) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val requestCode = ((reminder.id * 100) + index).toInt()

                val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                    action = ACTION_REMINDER
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                    putExtra(EXTRA_MEDICINE_ID, medicine.id)
                    putExtra(EXTRA_MEDICINE_NAME, medicine.brandName)
                    putExtra(EXTRA_DOSE, reminder.dose)
                    putExtra(EXTRA_BEFORE_FOOD, reminder.beforeFood)
                    putExtra(EXTRA_SCHEDULED_TIME, calendar.timeInMillis)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun scheduleSnooze(
        context: Context,
        reminderId: Long,
        medicineId: Long,
        medicineName: String,
        dose: String,
        beforeFood: Boolean,
        snoozeDurationMillis: Long = 10 * 60 * 1000L // 10 minutes
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + snoozeDurationMillis
        val requestCode = (reminderId * 1000 + 99).toInt()

        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_BEFORE_FOOD, beforeFood)
            putExtra(EXTRA_SCHEDULED_TIME, triggerTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (i in 0 until 10) {
            val requestCode = ((reminderId * 100) + i).toInt()
            val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                action = ACTION_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
