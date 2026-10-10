package com.curryplayer.quicksettingssoundprofile.composables

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RenderSoundModeSelectionCard(
    ctx: Context,
    dndPermissionGranted: Boolean,
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    iconTheme: IconTheme,
    onSelectedMode: (Int) -> Unit,
    previousRingerMode: Int,
    timerEndTime: Long,
    savedMuteDurationMinutes: Int,
    onSaveMuteDuration: (minutes: Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            RenderSoundModeSelection(
                selectedMode = selectedMode,
                iconTheme = iconTheme,
                onSelectedMode = onSelectedMode,
            )

            RenderTemporaryMuteSelection(
                scheduleExactAlarmsPermissionGranted = scheduleExactAlarmsPermissionGranted,
                selectedMode = selectedMode,
                previousRingerMode = previousRingerMode,
                isTimerActive = isTimerActive,
                timerEndTime = timerEndTime,
                currentTime = currentTime,
                savedMuteDurationMinutes = savedMuteDurationMinutes,
                onSaveMuteDuration = onSaveMuteDuration,
                onStartTimer = onStartTimer,
                onCancelTimer = onCancelTimer,
            )

        }
    }
}
