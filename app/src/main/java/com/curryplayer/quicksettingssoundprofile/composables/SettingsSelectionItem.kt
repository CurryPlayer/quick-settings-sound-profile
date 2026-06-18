package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curryplayer.quicksettingssoundprofile.R

@Composable
fun RenderSettingsSelectionItem(
    title: String,
    subtitle: String?,
    selectedOptionIndex: Int,
    onOptionSelected: (Int) -> Unit,
    hasPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasPermission) (if (isSystemInDarkTheme()) Color.White else Color.Black) else Color.Gray
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        iconDesignOptions.forEachIndexed { index, option ->
            val isSelected = index == selectedOptionIndex
            
            OutlinedCard(
                onClick = { if (hasPermission) onOptionSelected(index) },
                enabled = hasPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconColumn(option.ringIcon, stringResource(R.string.profile_sound_label), hasPermission)
                    IconColumn(option.vibrateIcon, stringResource(R.string.profile_vibrate_label), hasPermission)
                    IconColumn(option.silentIcon, stringResource(R.string.profile_silent_label), hasPermission)
                }
            }
        }
    }
}

@Composable
private fun IconColumn(iconRes: Int, label: String, isEnabled: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else Color.Gray
        )
    }
}

data class SelectionOption(
    val ringIcon: Int,
    val vibrateIcon: Int,
    val silentIcon: Int
)

val iconDesignOptions = listOf(
    // index 0
    SelectionOption(
        ringIcon = R.drawable.ic_round_volume_up_24,
        vibrateIcon = R.drawable.ic_round_vibration_24,
        silentIcon = R.drawable.ic_round_volume_off_24
    ),
    // index 1
    SelectionOption(
        ringIcon = R.drawable.ic_round_notifications_active_24,
        vibrateIcon = R.drawable.ic_round_vibration_outline_24,
        silentIcon = R.drawable.ic_round_notifications_off_24
    )
)
