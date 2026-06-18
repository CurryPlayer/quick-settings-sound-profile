package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RenderSettingsSelectionItem(
    title: String,
    subtitle: String?,
    options: List<SelectionOption>,
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
            color = if (hasPermission) (if (isSystemInDarkTheme()) Color.White else Color.Black) else Color.Gray
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasPermission) { onOptionSelected(index) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (index == selectedOptionIndex),
                    onClick = { onOptionSelected(index) },
                    enabled = hasPermission
                )
                
                Spacer(modifier = Modifier.width(12.dp))

                if (option.iconRes != null) {
                    Icon(
                        painter = painterResource(id = option.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (hasPermission) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = option.label,
                    fontSize = 16.sp,
                    color = if (hasPermission) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        }
    }
}

data class SelectionOption(
    val label: String,
    val iconRes: Int? = null
)
