package com.curryplayer.quicksettingssoundprofile.composables

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.curryplayer.quicksettingssoundprofile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTopAppBar(ctx: Context) {
    TopAppBar(
        title = {
            Text(
                text = ctx.getString(R.string.top_app_bar_title),
                color = if (isSystemInDarkTheme()) Color.White else Color.Black
            )
        }
    )
}
