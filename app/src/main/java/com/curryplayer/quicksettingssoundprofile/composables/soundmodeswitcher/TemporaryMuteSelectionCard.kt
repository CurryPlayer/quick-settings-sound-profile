package com.curryplayer.quicksettingssoundprofile.composables.soundmodeswitcher

import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption
import com.curryplayer.quicksettingssoundprofile.utils.TimeUtils

@Composable
fun RenderTemporaryMuteSelection(
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    previousRingerMode: Int,
    isTimerActive: Boolean,
    timerEndTime: Long,
    currentTime: Long,
    savedMuteDurationMinutes: Int,
    savedMuteDurationOption: MuteDurationOption,
    savedCustomMuteDurationMinutes: Int,
    onSaveMuteDuration: (minutes: Int) -> Unit,
    onSaveMuteDurationOption: (MuteDurationOption) -> Unit,
    onSaveCustomMuteDuration: (minutes: Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by remember(savedMuteDurationOption) {
        mutableStateOf(savedMuteDurationOption)
    }
    var customHours by remember(savedCustomMuteDurationMinutes) {
        mutableIntStateOf(savedCustomMuteDurationMinutes / 60)
    }
    var customMinutes by remember(savedCustomMuteDurationMinutes) {
        mutableIntStateOf(savedCustomMuteDurationMinutes % 60)
    }

    val finalMinutes = when (selectedOption) {
        MuteDurationOption.CUSTOM -> {
            val customTotal = (customHours * 60) + customMinutes
            if (customTotal > 0) customTotal else savedMuteDurationMinutes
        }
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

                        val context = LocalContext.current
                        val timeString = TimeUtils.getFormattedRemainingTime(context, timerEndTime, currentTime)

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
                RenderMuteDurationSelection(
                    selectedOption = selectedOption,
                    customHours = customHours,
                    customMinutes = customMinutes,
                    onOptionSelected = { option ->
                        selectedOption = option
                        onSaveMuteDurationOption(option)
                    },
                    onCustomTimeChanged = { hours, minutes ->
                        customHours = hours
                        customMinutes = minutes
                        onSaveCustomMuteDuration((hours * 60) + minutes)
                    },
                    onSaveMuteDuration = onSaveMuteDuration,
                    onStartTimer = onStartTimer,
                )
            }

        }
    }
}
