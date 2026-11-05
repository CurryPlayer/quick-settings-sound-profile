package com.curryplayer.quicksettingssoundprofile.composables.soundmodeswitcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.curryplayer.quicksettingssoundprofile.composables.RenderTimePickerDialog
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption
import com.curryplayer.quicksettingssoundprofile.utils.TimeUtils

@Composable
fun RenderMuteDurationSelection(
    selectedOption: MuteDurationOption,
    customHours: Int,
    customMinutes: Int,
    onOptionSelected: (MuteDurationOption) -> Unit,
    onCustomTimeChanged: (hours: Int, minutes: Int) -> Unit,
    onSaveMuteDuration: (minutes: Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    isTimerActive: Boolean = true,
) {
    var showTimePickerDialog by remember { mutableStateOf(value = false) }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Text(
            text = stringResource(R.string.select_duration),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val is30mSelected = selectedOption == MuteDurationOption.MINUTES_30
            AppFilterChip(
                selected = is30mSelected,
                onClick = {
                    onOptionSelected(MuteDurationOption.MINUTES_30)
                    onSaveMuteDuration(MuteDurationOption.MINUTES_30.minutes)
                    if (isTimerActive) {
                        onStartTimer(MuteDurationOption.MINUTES_30.minutes)
                    }
                },
                text = stringResource(R.string.duration_30m),
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.padding(horizontal = 4.dp))

            val is60mSelected = selectedOption == MuteDurationOption.MINUTES_60
            AppFilterChip(
                selected = is60mSelected,
                onClick = {
                    onOptionSelected(MuteDurationOption.MINUTES_60)
                    onSaveMuteDuration(MuteDurationOption.MINUTES_60.minutes)
                    if (isTimerActive) {
                        onStartTimer(MuteDurationOption.MINUTES_60.minutes)
                    }
                },
                text = stringResource(R.string.duration_1h),
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.padding(horizontal = 4.dp))

            val is180mSelected = selectedOption == MuteDurationOption.MINUTES_180
            AppFilterChip(
                selected = is180mSelected,
                onClick = {
                    onOptionSelected(MuteDurationOption.MINUTES_180)
                    onSaveMuteDuration(MuteDurationOption.MINUTES_180.minutes)
                    if (isTimerActive) {
                        onStartTimer(MuteDurationOption.MINUTES_180.minutes)
                    }
                },
                text = stringResource(R.string.duration_3h),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isCustomSelected = selectedOption == MuteDurationOption.CUSTOM
            val customTotalMinutes = (customHours * 60) + customMinutes
            val context = LocalContext.current
            val customText = if (customTotalMinutes > 0) {
                val formattedDuration = TimeUtils.getFormattedDuration(context, customTotalMinutes)
                stringResource(R.string.custom_duration_format, stringResource(R.string.custom_minutes), formattedDuration)
            } else {
                stringResource(R.string.custom_minutes)
            }

            AppFilterChip(
                selected = isCustomSelected,
                onClick = {
                    onOptionSelected(MuteDurationOption.CUSTOM)
                    showTimePickerDialog = true
                    if (customTotalMinutes > 0) {
                        onSaveMuteDuration(customTotalMinutes)
                        if (isTimerActive) {
                            onStartTimer(customTotalMinutes)
                        }
                    }
                },
                text = customText,
            )
        }

        if (showTimePickerDialog) {
            RenderTimePickerDialog(
                initialHours = customHours,
                initialMinutes = customMinutes,
                onDismiss = { showTimePickerDialog = false },
                onConfirm = { hours, minutes ->
                    onCustomTimeChanged(hours, minutes)
                    val duration = (hours * 60) + minutes
                    onSaveMuteDuration(duration)
                    if (isTimerActive) {
                        onStartTimer(duration)
                    }
                    showTimePickerDialog = false
                },
            )
        }
    }
}
