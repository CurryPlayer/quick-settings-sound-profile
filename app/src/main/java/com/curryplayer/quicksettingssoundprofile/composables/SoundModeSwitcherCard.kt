package com.curryplayer.quicksettingssoundprofile.composables

import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.IconTheme

@Composable
fun RenderSoundModeSelection(
    selectedMode: Int,
    iconTheme: IconTheme,
    onSelectedMode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.sound_mode_selection_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sound Mode Card
            SoundModeCard(
                title = stringResource(R.string.profile_sound_label),
                iconRes = iconTheme.ringIcon,
                isSelected = selectedMode == AudioManager.RINGER_MODE_NORMAL,
                onClick = {
                    onSelectedMode(AudioManager.RINGER_MODE_NORMAL)
                },
                modifier = Modifier.weight(1f)
            )

            // Vibrate Mode Card
            SoundModeCard(
                title = stringResource(R.string.profile_vibrate_label),
                iconRes = iconTheme.vibrateIcon,
                isSelected = selectedMode == AudioManager.RINGER_MODE_VIBRATE,
                onClick = {
                    onSelectedMode(AudioManager.RINGER_MODE_VIBRATE)
                },
                modifier = Modifier.weight(1f)
            )

            // Mute / Silent Mode Card
            SoundModeCard(
                title = stringResource(R.string.profile_silent_label),
                iconRes = iconTheme.silentIcon,
                isSelected = selectedMode == AudioManager.RINGER_MODE_SILENT,
                onClick = {
                    onSelectedMode(AudioManager.RINGER_MODE_SILENT)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SoundModeCard(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                alpha = 0.5f
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}
