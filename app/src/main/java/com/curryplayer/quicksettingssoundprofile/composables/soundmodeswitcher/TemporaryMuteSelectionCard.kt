package com.curryplayer.quicksettingssoundprofile.composables.soundmodeswitcher

import android.media.AudioManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.composables.RenderSettingsSwitchRow
import com.curryplayer.quicksettingssoundprofile.data.rememberDataStoreManager
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption
import com.curryplayer.quicksettingssoundprofile.utils.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RenderTemporaryMuteSelection(
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataStoreManager = rememberDataStoreManager()
    val scope = rememberCoroutineScope()

    val timerEndTime by dataStoreManager.timerEndTime.collectAsState(initial = 0L)
    val previousRingerMode by dataStoreManager.previousRingerMode.collectAsState(initial = AudioManager.RINGER_MODE_NORMAL)
    val savedMuteDurationMinutes by dataStoreManager.lastMuteDurationMinutes.collectAsState(initial = MuteDurationOption.MINUTES_60.minutes)
    val savedMuteDurationOption by dataStoreManager.lastMuteDurationOption.collectAsState(initial = MuteDurationOption.MINUTES_60)
    val savedCustomMuteDurationMinutes by dataStoreManager.customMuteDurationMinutes.collectAsState(initial = MuteDurationOption.CUSTOM.minutes)

    val onSaveMuteDuration: (Int) -> Unit = { minutes ->
        scope.launch { dataStoreManager.saveMuteDurationMinutes(minutes) }
    }
    val onSaveMuteDurationOption: (MuteDurationOption) -> Unit = { option ->
        scope.launch { dataStoreManager.saveMuteDurationOption(option) }
    }
    val onSaveCustomMuteDuration: (Int) -> Unit = { minutes ->
        scope.launch { dataStoreManager.saveCustomMuteDurationMinutes(minutes) }
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timerEndTime) {
        while (isActive) {
            val now = System.currentTimeMillis()
            if (timerEndTime <= now) break
            currentTime = now

            val remainingMillis = timerEndTime - now
            val millisUntilNextMinute = remainingMillis % 60_000L
            val nextDelay = if (millisUntilNextMinute == 0L) 60_000L else millisUntilNextMinute
            delay(nextDelay.milliseconds)
        }
    }

    val isTimerActive = timerEndTime > currentTime

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

            val targetModeLabel = when (previousRingerMode) {
                AudioManager.RINGER_MODE_VIBRATE -> stringResource(R.string.profile_vibrate_label)
                else -> stringResource(R.string.profile_sound_label)
            }

            val statusText = if (isTimerActive) {
                val timeString = TimeUtils.getFormattedRemainingTime(context, timerEndTime, currentTime)
                stringResource(R.string.time_remaining, timeString)
            } else {
                null
            }

            RenderSettingsSwitchRow(
                title = stringResource(R.string.temporary_mute_title),
                description = stringResource(R.string.temporary_mute_desc, targetModeLabel),
                statusText = statusText,
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
