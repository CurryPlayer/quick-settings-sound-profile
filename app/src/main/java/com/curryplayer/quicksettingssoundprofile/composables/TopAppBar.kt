package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTopAppBar() {
    TopAppBar(
        title = { Text("Quick Settings Sound Profile") }
    )
}