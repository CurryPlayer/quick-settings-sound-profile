package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    initialHours: Int = 0,
    initialMinutes: Int = 0,
    is24Hour: Boolean = true,
    title: String = stringResource(R.string.set_duration_title),
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHours,
        initialMinute = initialMinutes,
        is24Hour = is24Hour,
    )

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
                        onConfirm(timePickerState.hour, timePickerState.minute)
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
        TimePicker(
            state = timePickerState,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
