package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.IconTheme

@Composable
fun RenderIconThemeSelector(
    selectedOptionIndex: Int,
    onOptionSelectedIndex: (Int) -> Unit,
    hasPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        IconTheme.entries.forEachIndexed { index, option ->
            val isSelected = index == selectedOptionIndex
            IconTheme(hasPermission, onOptionSelectedIndex, index, isSelected, option)
        }
    }
}

@Composable
private fun IconTheme(
    hasPermission: Boolean,
    onOptionSelectedIndex: (Int) -> Unit,
    index: Int,
    isSelected: Boolean,
    option: IconTheme
) {
    OutlinedCard(
        onClick = { if (hasPermission) onOptionSelectedIndex(index) },
        enabled = hasPermission,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                alpha = 0.5f
            )
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
            IconAndTextColumn(option.ringIcon, stringResource(R.string.profile_sound_label), hasPermission)
            IconAndTextColumn(option.vibrateIcon, stringResource(R.string.profile_vibrate_label), hasPermission)
            IconAndTextColumn(option.silentIcon, stringResource(R.string.profile_silent_label), hasPermission
            )
        }
    }
}

@Composable
private fun IconAndTextColumn(iconRes: Int, label: String, isEnabled: Boolean) {
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


