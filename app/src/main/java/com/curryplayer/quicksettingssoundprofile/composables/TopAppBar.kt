package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTopAppBar() {
    TopAppBar(
        title = {
            Text(
                text = "Quick Settings Sound Profile",
                color = if (isSystemInDarkTheme()) Color.White else Color.Black
            )
        }
    )
}