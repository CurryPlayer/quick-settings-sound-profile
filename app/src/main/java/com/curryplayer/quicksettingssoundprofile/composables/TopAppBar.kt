package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.curryplayer.quicksettingssoundprofile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTopAppBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.top_app_bar_title),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    )
}
