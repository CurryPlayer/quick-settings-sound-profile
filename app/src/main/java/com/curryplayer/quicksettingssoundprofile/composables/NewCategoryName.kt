package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RenderNewCategoryName(title: String) {
    Text(
        text = title,
        color = if (isSystemInDarkTheme()) Color.White else Color.Black,
        modifier = Modifier.padding(16.dp)
    )
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}