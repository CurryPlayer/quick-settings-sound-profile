package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    initialHours: Int = 0,
    initialMinutes: Int = MuteDurationOption.MINUTES_30.minutes,    // should be greater 0 to avoid potential issues
    is24Hour: Boolean = true,
    title: String = stringResource(R.string.set_duration_title),
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHours,
        initialMinute = initialMinutes,
        is24Hour = is24Hour,
    )
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        if (timePickerState.hour > 0 || timePickerState.minute > 0) {
            showError = false
        }
    }

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppButton(
                    text = stringResource(R.string.alert_button_cancel),
                    onClick = onDismiss,
                    type = AppButtonType.OUTLINED,
                )
                AppButton(
                    text = stringResource(R.string.alert_button_apply),
                    onClick = {
                        if (timePickerState.hour == 0 && timePickerState.minute == 0) {
                            showError = true
                        } else {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                        }
                    },
                    type = AppButtonType.FILLED,
                )
            }
        },
        dismissButton = {
            // unused
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (showError) {
                Text(
                    text = stringResource(R.string.timer_min_duration_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}
