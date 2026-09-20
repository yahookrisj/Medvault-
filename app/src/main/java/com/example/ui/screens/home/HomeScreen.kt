package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TodayDoseItem
import com.example.ui.components.AdherenceProgressRing
import com.example.ui.components.DosageFormBadge
import com.example.ui.components.MedicineThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.components.StockBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.MedVaultViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MedVaultViewModel,
    onNavigateToMedicineDetail: (Long) -> Unit,
    onNavigateToAddMedicine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCalendar by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val todayDoses by viewModel.todayDoses.collectAsStateWithLifecycle()

    val totalDoses = todayDoses.size
    val completedDoses = todayDoses.count { it.status == "TAKEN" }
    val adherenceFraction = if (totalDoses > 0) completedDoses.toFloat() / totalDoses else 0f

    val upcomingDoses = todayDoses.filter { it.status == "PENDING" }
    val missedDoses = todayDoses.filter { it.status == "MISSED" }
    val finishedDoses = todayDoses.filter { it.status == "TAKEN" || it.status == "SKIPPED" }

    val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
    val isToday = isSameDay(selectedCalendar, Calendar.getInstance())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Date Selector
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isToday) "Today" else dateFormat.format(selectedCalendar.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (!isToday) {
                            TextButton(
                                onClick = { viewModel.resetToToday() },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Go to Today", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (isToday) {
                        Text(
                            text = dateFormat.format(selectedCalendar.time),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Navigation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.changeDateOffset(-1) },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yesterday")
                        }

                        OutlinedButton(
                            onClick = { viewModel.changeDateOffset(1) },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Tomorrow")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Adherence Card with Progress Ring
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Adherence",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                totalDoses == 0 -> "No doses scheduled for this day"
                                completedDoses == totalDoses -> "All doses completed! Great job!"
                                completedDoses > 0 -> "$completedDoses completed, ${totalDoses - completedDoses} remaining"
                                else -> "Ready to take your scheduled doses"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        if (totalDoses == 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToAddMedicine,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Medicine")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    AdherenceProgressRing(
                        percentage = adherenceFraction,
                        completedCount = completedDoses,
                        totalCount = totalDoses,
                        size = 110.dp,
                        strokeWidth = 10.dp
                    )
                }
            }
        }

        // Missed Doses Section
        if (missedDoses.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Missed Doses (${missedDoses.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AlertRed
                        )
                    )
                }
            }

            items(missedDoses, key = { "missed_${it.reminderId}_${it.scheduledTimeMillis}" }) { doseItem ->
                DoseCard(
                    doseItem = doseItem,
                    onTakeClick = { viewModel.markDoseTaken(doseItem) },
                    onSkipClick = { viewModel.markDoseSkipped(doseItem) },
                    onSnoozeClick = { viewModel.snoozeDose(doseItem) },
                    onCardClick = { onNavigateToMedicineDetail(doseItem.medicineId) }
                )
            }
        }

        // Upcoming Doses Section
        item {
            Text(
                text = "Upcoming Doses (${upcomingDoses.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (upcomingDoses.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (totalDoses > 0) "No more pending doses for today!" else "No reminders scheduled for this date.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        } else {
            items(upcomingDoses, key = { "upcoming_${it.reminderId}_${it.scheduledTimeMillis}" }) { doseItem ->
                DoseCard(
                    doseItem = doseItem,
                    onTakeClick = { viewModel.markDoseTaken(doseItem) },
                    onSkipClick = { viewModel.markDoseSkipped(doseItem) },
                    onSnoozeClick = { viewModel.snoozeDose(doseItem) },
                    onCardClick = { onNavigateToMedicineDetail(doseItem.medicineId) }
                )
            }
        }

        // Completed / Skipped Doses Section
        if (finishedDoses.isNotEmpty()) {
            item {
                Text(
                    text = "Completed & Skipped (${finishedDoses.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(finishedDoses, key = { "finished_${it.reminderId}_${it.scheduledTimeMillis}" }) { doseItem ->
                DoseCard(
                    doseItem = doseItem,
                    onTakeClick = { viewModel.markDoseTaken(doseItem) },
                    onSkipClick = { viewModel.markDoseSkipped(doseItem) },
                    onSnoozeClick = { viewModel.snoozeDose(doseItem) },
                    onCardClick = { onNavigateToMedicineDetail(doseItem.medicineId) }
                )
            }
        }
    }
}

@Composable
fun DoseCard(
    doseItem: TodayDoseItem,
    onTakeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("dose_card_${doseItem.medicineId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Medicine Image / Icon Thumbnail
                MedicineThumbnail(
                    imagePath = doseItem.frontImagePath,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = doseItem.medicineBrandName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        StatusBadge(status = doseItem.status)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${doseItem.medicineGenericName} • ${doseItem.strength}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DosageFormBadge(dosageForm = doseItem.dosageForm)
                        StockBadge(quantity = doseItem.remainingStock, threshold = 5)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time & Guideline Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = doseItem.timeFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Dose: ${doseItem.dose}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (doseItem.beforeFood) "Before food" else "After food",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }

            // Quick Actions if Pending or Missed
            if (doseItem.status == "PENDING" || doseItem.status == "MISSED") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTakeClick,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("take_button_${doseItem.medicineId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Now")
                    }

                    OutlinedButton(
                        onClick = onSnoozeClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze")
                    }

                    FilledTonalButton(
                        onClick = onSkipClick,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skip")
                    }
                }
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
