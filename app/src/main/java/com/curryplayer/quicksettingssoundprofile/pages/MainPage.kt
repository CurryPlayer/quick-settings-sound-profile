package com.curryplayer.quicksettingssoundprofile.pages

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.composables.RenderAddTileToStatusBarCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderGrantPermissionCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderIconThemeSelector
import com.curryplayer.quicksettingssoundprofile.composables.RenderNewCategoryName
import com.curryplayer.quicksettingssoundprofile.composables.RenderNoUserManagedModesAvailableCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderZenModeSettingsNoticeCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderOpenZenModeSettings
import com.curryplayer.quicksettingssoundprofile.composables.RenderTopAppBar
import com.curryplayer.quicksettingssoundprofile.composables.soundmodeswitcher.RenderSoundModeSelectionCard
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption
import com.curryplayer.quicksettingssoundprofile.models.ZenModesSupport

/**
 * Main page layout:
 *
 * 1. Permission      - only without DND permission
 * 2. Sound profile   - sound mode selection (+ temporary mute), only with permission
 * 3. Quick access    - "add tile" card, only with permission and while the tile is not placed
 * 4. Silent mode     - notice / zen mode settings (depends on Android version and device)
 * 5. Appearance      - icon design (greyed out without permission)
 */
@Composable
fun RenderMainPage(
    ctx: Context,
    hasPermission: Boolean,
    tileAdded: Boolean,
    onTileAddedChanged: (Boolean) -> Unit,
    ruleId: String,
    iconThemeIndex: Int,
    onIconThemeChangeIndex: (Int) -> Unit,
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    onSelectedMode: (Int) -> Unit,
    previousRingerMode: Int,
    savedMuteDurationMinutes: Int,
    savedMuteDurationOption: MuteDurationOption,
    savedCustomMuteDurationMinutes: Int,
    onSaveMuteDuration: (minutes: Int) -> Unit,
    onSaveMuteDurationOption: (MuteDurationOption) -> Unit,
    onSaveCustomMuteDuration: (minutes: Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zenModeSupport = remember(ctx) { ZenModesSupport.resolveDeviceZenModesSupport(ctx) }
    val showAddTileCard = hasPermission && !tileAdded
    val iconTheme = IconTheme.fromOrdinal(iconThemeIndex)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { RenderTopAppBar(ctx) },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!hasPermission) {
                    PermissionSection(ctx)
                } else {
                    SoundProfileSection(
                        ctx = ctx,
                        scheduleExactAlarmsPermissionGranted = scheduleExactAlarmsPermissionGranted,
                        selectedMode = selectedMode,
                        iconTheme = iconTheme,
                        onSelectedMode = onSelectedMode,
                        previousRingerMode = previousRingerMode,
                        savedMuteDurationMinutes = savedMuteDurationMinutes,
                        savedMuteDurationOption = savedMuteDurationOption,
                        savedCustomMuteDurationMinutes = savedCustomMuteDurationMinutes,
                        onSaveMuteDuration = onSaveMuteDuration,
                        onSaveMuteDurationOption = onSaveMuteDurationOption,
                        onSaveCustomMuteDuration = onSaveCustomMuteDuration,
                        onStartTimer = onStartTimer,
                        onCancelTimer = onCancelTimer,
                    )

                    if (showAddTileCard) {
                        AddQuickSettingsTileSection(
                            ctx = ctx,
                            iconTheme = iconTheme,
                        ) { onTileAddedChanged(true) }
                    }
                }

                SilentModeSettingsSection(ctx, zenModeSupport, hasPermission, ruleId)

                Spacer(Modifier.height(16.dp))

                IconThemeSelectionSection(
                    ctx = ctx,
                    iconThemeIndex = iconThemeIndex,
                    onIconThemeChangeIndex = onIconThemeChangeIndex,
                    hasPermission = hasPermission,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    )
}

@Composable
private fun PermissionSection(ctx: Context) {
    RenderGrantPermissionCard(ctx)
}

@Composable
private fun SoundProfileSection(
    ctx: Context,
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    iconTheme: IconTheme,
    onSelectedMode: (Int) -> Unit,
    previousRingerMode: Int,
    savedMuteDurationMinutes: Int,
    savedMuteDurationOption: MuteDurationOption,
    savedCustomMuteDurationMinutes: Int,
    onSaveMuteDuration: (minutes: Int) -> Unit,
    onSaveMuteDurationOption: (MuteDurationOption) -> Unit,
    onSaveCustomMuteDuration: (minutes: Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
) {
    RenderNewCategoryName(
        title = ctx.getString(R.string.category_sound_mode_title),
        description = null,
    )
    RenderSoundModeSelectionCard(
        scheduleExactAlarmsPermissionGranted = scheduleExactAlarmsPermissionGranted,
        selectedMode = selectedMode,
        iconTheme = iconTheme,
        onSelectedMode = onSelectedMode,
        previousRingerMode = previousRingerMode,
        savedMuteDurationMinutes = savedMuteDurationMinutes,
        savedMuteDurationOption = savedMuteDurationOption,
        savedCustomMuteDurationMinutes = savedCustomMuteDurationMinutes,
        onSaveMuteDuration = onSaveMuteDuration,
        onSaveMuteDurationOption = onSaveMuteDurationOption,
        onSaveCustomMuteDuration = onSaveCustomMuteDuration,
        onStartTimer = onStartTimer,
        onCancelTimer = onCancelTimer,
    )
}

@Composable
private fun AddQuickSettingsTileSection(
    ctx: Context,
    iconTheme: IconTheme,
    onTileAdded: () -> Unit,
) {
    RenderNewCategoryName(
        title = ctx.getString(R.string.section_quick_access_title),
        description = null,
    )
    RenderAddTileToStatusBarCard(ctx, iconTheme, onTileAdded)
}

@Composable
private fun SilentModeSettingsSection(
    ctx: Context,
    zenModes: ZenModesSupport,
    hasPermission: Boolean,
    ruleId: String,
) {
    // only render this section on Android 10 and above as the ZenMode policy is available on these versions onwards
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

    RenderNewCategoryName(
        title = ctx.getString(R.string.category_silent_mode_title),
        description = ctx.getString(R.string.category_silent_mode_description),
    )

    // versions below Android 15 only show notice card for ZenMode / ZenRule settings but customization is not possible on these versions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        ZenModesSettings(ctx, zenModes, hasPermission, ruleId)
    } else {
        RenderZenModeSettingsNoticeCard(ctx)
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
private fun ZenModesSettings(
    ctx: Context,
    zenModes: ZenModesSupport,
    hasPermission: Boolean,
    ruleId: String,
) {
    when (zenModes) {
        // with userManaged modes, the user can press the button to open the ZenMode settings directly
        ZenModesSupport.UserManaged -> RenderZenModeSettingsNoticeCard(
            ctx = ctx,
            actions = { RenderOpenZenModeSettings(ctx, ruleId, hasPermission) },
        )

        // if no userManged modes are available on Android 15, show only the notice card and additionally the limitation card
        else -> {
            RenderZenModeSettingsNoticeCard(ctx)
            RenderNoUserManagedModesAvailableCard(ctx)
        }
    }
}

@Composable
private fun IconThemeSelectionSection(
    ctx: Context,
    iconThemeIndex: Int,
    onIconThemeChangeIndex: (Int) -> Unit,
    hasPermission: Boolean,
) {
    RenderNewCategoryName(
        title = ctx.getString(R.string.category_icon_design_title),
        description = ctx.getString(R.string.category_icon_design_description),
    )

    RenderIconThemeSelector(
        selectedOptionIndex = iconThemeIndex,
        onOptionSelectedIndex = onIconThemeChangeIndex,
        hasPermission = hasPermission
    )
}
