package com.curryplayer.quicksettingssoundprofile.pages

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.curryplayer.quicksettingssoundprofile.composables.RenderAllSetCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderGrantPermissionCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderNoUserManagedModesAvailableCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderNoticeCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderTopAppBar
import com.curryplayer.quicksettingssoundprofile.composables.RenderOpenZenModeSettings

@Composable
fun RenderSettingsPage(
    ctx: Context,
    hasPermission: Boolean,
    ruleId: String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { RenderTopAppBar(ctx) },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            ) {

                if (!hasPermission) {
                    RenderGrantPermissionCard(ctx)
                } else {
                    RenderAllSetCard(ctx)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    RenderNoticeCard(ctx)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    // Not every device running Android 15 has user managed modes. These devices use schedules instead, which cannot be modified directly.
                    val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (!notificationManager.areAutomaticZenRulesUserManaged()) {
                        RenderNoUserManagedModesAvailableCard(ctx)
                    }
                    RenderOpenZenModeSettings(ctx, ruleId, hasPermission)
                }

            }
        }
    )
}
