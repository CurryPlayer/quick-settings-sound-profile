package com.curryplayer.quicksettingssoundprofile.composables

import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTemporaryMuteSelection(
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    previousRingerMode: Int,
    isTimerActive: Boolean,
    timerEndTime: Long,
    currentTime: Long,
    savedMuteDurationMinutes: Int,
    onSaveMuteDuration: (minutes: Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by remember(savedMuteDurationMinutes) {
        mutableStateOf(MuteDurationOption.fromMinutes(savedMuteDurationMinutes))
    }
    var customHours by remember(savedMuteDurationMinutes) {
        mutableIntStateOf(savedMuteDurationMinutes / 60)
    }
    var customMinutes by remember(savedMuteDurationMinutes) {
        mutableIntStateOf(savedMuteDurationMinutes % 60)
    }
    var showTimeSelectorDialog by remember { mutableStateOf(value = false) }

    val finalMinutes = when (selectedOption) {
        MuteDurationOption.CUSTOM -> (customHours * 60) + customMinutes
        else -> selectedOption.minutes
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {

        if (selectedMode == AudioManager.RINGER_MODE_SILENT) {

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            if (!scheduleExactAlarmsPermissionGranted) {
                RenderGrantExactAlarmPermission()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.temporary_mute_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val targetModeLabel = when (previousRingerMode) {
                        AudioManager.RINGER_MODE_VIBRATE -> stringResource(R.string.profile_vibrate_label)
                        else -> stringResource(R.string.profile_sound_label)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.temporary_mute_desc, targetModeLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isTimerActive) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val remainingMillis = (timerEndTime - currentTime).coerceAtLeast(0L)
                        val totalMinutes = (remainingMillis + 59_999L) / 60_000L
                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60

                        val timeString = when {
                            (hours > 0) && (minutes > 0) -> stringResource(R.string.time_format_hours_minutes, hours, minutes)
                            hours > 0 -> stringResource(R.string.time_format_hours_only, hours)
                            else -> stringResource(R.string.time_format_minutes_only, minutes)
                        }

                        Text(
                            text = stringResource(R.string.time_remaining, timeString),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    }
                }

                VerticalDivider(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 2.dp
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Switch(
                    checked = isTimerActive,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            onSaveMuteDuration(finalMinutes)
                            onStartTimer(finalMinutes)
                        } else {
                            onCancelTimer()
                        }
                    }
                )
            }

            if (isTimerActive) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = stringResource(R.string.select_duration),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val is30mSelected = selectedOption == MuteDurationOption.MINUTES_30
                    FilterChip(
                        selected = is30mSelected,
                        onClick = {
                            selectedOption = MuteDurationOption.MINUTES_30
                            onSaveMuteDuration(MuteDurationOption.MINUTES_30.minutes)
                            if (isTimerActive) {
                                onStartTimer(MuteDurationOption.MINUTES_30.minutes)
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.duration_30m),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = is30mSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                    val is60mSelected = selectedOption == MuteDurationOption.MINUTES_60
                    FilterChip(
                        selected = is60mSelected,
                        onClick = {
                            selectedOption = MuteDurationOption.MINUTES_60
                            onSaveMuteDuration(MuteDurationOption.MINUTES_60.minutes)
                            if (isTimerActive) {
                                onStartTimer(MuteDurationOption.MINUTES_60.minutes)
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.duration_1h),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = is60mSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                    val is180mSelected = selectedOption == MuteDurationOption.MINUTES_180
                    FilterChip(
                        selected = is180mSelected,
                        onClick = {
                            selectedOption = MuteDurationOption.MINUTES_180
                            onSaveMuteDuration(MuteDurationOption.MINUTES_180.minutes)
                            if (isTimerActive) {
                                onStartTimer(MuteDurationOption.MINUTES_180.minutes)
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.duration_3h),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = is180mSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCustomSelected = selectedOption == MuteDurationOption.CUSTOM
                    FilterChip(
                        selected = isCustomSelected,
                        onClick = {
                            selectedOption = MuteDurationOption.CUSTOM
                            showTimeSelectorDialog = true
                            val duration = (customHours * 60) + customMinutes
                            if (duration > 0) {
                                onSaveMuteDuration(duration)
                                if (isTimerActive) {
                                    onStartTimer(duration)
                                }
                            }
                        },
                        label = {
                            val customText = if (isCustomSelected && (finalMinutes > 0)) {
                                val h = finalMinutes / 60
                                val m = finalMinutes % 60
                                val formattedDuration = when {
                                    (h > 0) && (m > 0) -> stringResource(R.string.time_format_hours_minutes, h, m)
                                    h > 0 -> stringResource(R.string.time_format_hours_only, h)
                                    else -> stringResource(R.string.time_format_minutes_only, m)
                                }
                                stringResource(R.string.custom_duration_format, stringResource(R.string.custom_minutes), formattedDuration)
                            } else {
                                stringResource(R.string.custom_minutes)
                            }
                            Text(
                                text = customText,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isCustomSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }

                if (showTimeSelectorDialog) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = customHours,
                        initialMinute = customMinutes,
                        is24Hour = true
                    )

                    TimePickerDialog(
                        onDismissRequest = { showTimeSelectorDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.set_duration_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        confirmButton = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AppButton(
                                    text = stringResource(R.string.alert_button_cancel),
                                    onClick = {
                                        showTimeSelectorDialog = false
                                    },
                                    type = AppButtonType.OUTLINED
                                )
                                AppButton(
                                    text = stringResource(R.string.alert_button_apply),
                                    onClick = {
                                        customHours = timePickerState.hour
                                        customMinutes = timePickerState.minute
                                        val duration = timePickerState.hour * 60 + timePickerState.minute
                                        onSaveMuteDuration(duration)
                                        if (isTimerActive) {
                                            onStartTimer(duration)
                                        }
                                        showTimeSelectorDialog = false
                                    },
                                    type = AppButtonType.FILLED
                                )
                            }
                        },
                        dismissButton = {
                            // unused
                        }
                    ) {
                        TimePicker(
                            state = timePickerState,
                            modifier = Modifier
                                .padding(top = 16.dp)
                        )
                    }
                }
            }

        }
    }
}
