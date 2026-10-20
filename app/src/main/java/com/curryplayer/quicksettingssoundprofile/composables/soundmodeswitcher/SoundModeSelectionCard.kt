package com.curryplayer.quicksettingssoundprofile.composables.soundmodeswitcher

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption

@Composable
fun RenderSoundModeSelectionCard(
    ctx: Context,
    dndPermissionGranted: Boolean,
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    iconTheme: IconTheme,
    onSelectedMode: (Int) -> Unit,
    previousRingerMode: Int,
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
                savedMuteDurationMinutes = savedMuteDurationMinutes,
                savedMuteDurationOption = savedMuteDurationOption,
                savedCustomMuteDurationMinutes = savedCustomMuteDurationMinutes,
                onSaveMuteDuration = onSaveMuteDuration,
                onSaveMuteDurationOption = onSaveMuteDurationOption,
                onSaveCustomMuteDuration = onSaveCustomMuteDuration,
                onStartTimer = onStartTimer,
                onCancelTimer = onCancelTimer,
            )

        }
    }
}
