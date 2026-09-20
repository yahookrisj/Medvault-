package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val database = AppDatabase.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                val activeReminders = database.reminderDao().getActiveRemindersList()
                for (reminder in activeReminders) {
                    val medicine = database.medicineDao().getMedicineById(reminder.medicineId)
                    if (medicine != null) {
                        AlarmScheduler.scheduleReminder(context, reminder, medicine)
                    }
                }
            }
        }
    }
}
