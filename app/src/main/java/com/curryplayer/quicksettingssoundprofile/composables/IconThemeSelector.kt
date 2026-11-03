package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.models.IconTheme

private const val COLUMNS = 2

/**
 * Non-scrolling grid of icon themes with [COLUMNS] cards per row. Each theme is a compact card
 * showing its sound, vibrate and silent icon next to each other.
 */
@Composable
fun RenderIconThemeSelector(
    selectedOptionIndex: Int,
    onOptionSelectedIndex: (Int) -> Unit,
    hasPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTheme.entries.withIndex().chunked(COLUMNS).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (index, option) ->
                    IconThemeCard(
                        hasPermission = hasPermission,
                        onClick = { onOptionSelectedIndex(index) },
                        isSelected = index == selectedOptionIndex,
                        option = option,
                        modifier = Modifier.weight(1f),
                    )
                }
                // keep the width of the cards equal if the last row is not full
                repeat(COLUMNS - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IconThemeCard(
    hasPermission: Boolean,
    onClick: () -> Unit,
    isSelected: Boolean,
    option: IconTheme,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = hasPermission,
        modifier = modifier.fillMaxWidth().semantics { selected = isSelected },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeIcon(option.ringIcon, hasPermission)
            ThemeIcon(option.vibrateIcon, hasPermission)
            ThemeIcon(option.silentIcon, hasPermission)
        }
    }
}

@Composable
private fun ThemeIcon(iconRes: Int, isEnabled: Boolean) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
    )
}
