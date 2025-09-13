package com.curryplayer.quicksettingssoundprofile.pages

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.curryplayer.quicksettingssoundprofile.composables.RenderGrantPermissionCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderTopAppBar
import com.curryplayer.quicksettingssoundprofile.composables.RenderNewCategoryName
import com.curryplayer.quicksettingssoundprofile.composables.RenderSettingsToggleItem

//@Preview(showBackground = true)
@Composable
//fun RenderSettingsPage(modifier: Modifier = Modifier.Companion) {
fun RenderSettingsPage(ctx: Context, hasPermission: Boolean, modifier: Modifier = Modifier.Companion) {
    Scaffold(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(color = Color.Companion.DarkGray),
        topBar = { RenderTopAppBar() },
        content = { contentPadding ->
            Column(
                modifier = Modifier.Companion
                    .padding(contentPadding)
            ) {
                if (!hasPermission) {
                    Log.i("Permission", "Not granted")
                    RenderGrantPermissionCard(ctx)
                }
                RenderNewCategoryName("Mute Settings")
                RenderSettingsToggleItem(
                    "Activate 'Do not disturb' mode on mute",
                    "If enabled, 'Do not disturb' mode will be activated when the device is muted",
                    false
                )
                RenderSettingsToggleItem(
                    "Also mute media",
                    "If enabled, media will be muted when the device is muted",
                    false
                )
            }
        }
    )
}