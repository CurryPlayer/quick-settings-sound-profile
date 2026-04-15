package com.curryplayer.quicksettingssoundprofile.pages

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.curryplayer.quicksettingssoundprofile.composables.RenderAllSetCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderGrantPermissionCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderTopAppBar
import com.curryplayer.quicksettingssoundprofile.composables.RenderNewCategoryName
import com.curryplayer.quicksettingssoundprofile.composables.RenderSettingsToggleItem
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.composables.RenderOpenZenModeSettings

@Composable
fun RenderSettingsPage(
    ctx: Context,
    hasPermission: Boolean,
    activateDnd: Boolean,
    onActivateDndChange: (Boolean) -> Unit,
    muteMedia: Boolean,
    onMuteMediaChange: (Boolean) -> Unit,
    ruleId: String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.DarkGray),
        topBar = { RenderTopAppBar(ctx) },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
            ) {
                if (!hasPermission) {
                    RenderGrantPermissionCard(ctx)
                } else {
                    RenderAllSetCard(ctx)
                }
//                TODO: Maybe enable in a future version when AutomaticZenRule is applied. For now, make phone completely silent (including media)
//                RenderNewCategoryName(ctx.getString(R.string.mute_settings_category))
//                RenderSettingsToggleItem(
//                    ctx.getString(R.string.mute_media_title),
//                    ctx.getString(R.string.mute_media_subtitle),
//                    muteMedia,
//                    onMuteMediaChange,
//                    hasPermission
//                )
//                TODO: See if it is possible to look for the current mode (maybe AutomaticZenRule >= Android 15)
//                RenderSettingsToggleItem(
//                    "Restore previous mode",
//                    "If enabled, the system tries to retore the last set mode after 'Do not disturb' mode is disabled. The system activates this mode automatically when the device gets muted.",
//                    activateDnd,
//                    onActivateDndChange,
//                    hasPermission
//                )
                // TODO: investigate if only Android 15 and higher can use this feature
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    RenderOpenZenModeSettings(ctx, ruleId, hasPermission)
                }
            }
        }
    )
}
