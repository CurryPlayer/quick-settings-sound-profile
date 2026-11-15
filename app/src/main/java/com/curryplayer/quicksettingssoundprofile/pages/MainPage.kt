package com.curryplayer.quicksettingssoundprofile.pages

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.composables.RenderAddTileToStatusBarCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderGrantPermissionCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderIconThemeSelector
import com.curryplayer.quicksettingssoundprofile.composables.RenderNewCategoryName
import com.curryplayer.quicksettingssoundprofile.composables.RenderNoUserManagedModesAvailableCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderZenModeSettingsNoticeCard
import com.curryplayer.quicksettingssoundprofile.composables.RenderOpenZenModeSettings
import com.curryplayer.quicksettingssoundprofile.composables.RenderSettingsSwitchRow
import com.curryplayer.quicksettingssoundprofile.composables.RenderTopAppBar
import com.curryplayer.quicksettingssoundprofile.composables.SectionCard
import com.curryplayer.quicksettingssoundprofile.composables.soundmodeswitcher.RenderSoundModeSelectionCard
import com.curryplayer.quicksettingssoundprofile.data.rememberDataStoreManager
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.models.ZenModesSupport
import kotlinx.coroutines.launch

/**
 * Main page layout:
 *
 * 1. Permission      - only without DND permission
 * 2. Sound profile   - sound mode selection (+ temporary mute), only with permission
 * 3. Quick access    - "add tile" card, only with permission and while the tile is not placed
 * 4. Silent mode     - notice / zen mode settings (depends on Android version and device)
 * 5. Appearance      - icon design (greyed out without permission)
 *
 * Only state that is shared between sections or driven by the Activity (permissions, ringer mode,
 * alarm scheduling) is passed in. Everything that is used by a single section is read from the
 * DataStore directly in that section.
 */
@Composable
fun RenderMainPage(
    hasPermission: Boolean,
    onIconThemeChangeIndex: (Int) -> Unit,
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    onSelectedMode: (Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataStoreManager = rememberDataStoreManager()
    // The icon theme is shared by several sections, so it is read once here
    val iconThemeIndex by dataStoreManager.iconTheme.collectAsState(initial = IconTheme.VOLUME_DEFAULT.ordinal)
    val iconTheme = IconTheme.fromOrdinal(iconThemeIndex)
    val zenModeSupport = remember(context) { ZenModesSupport.resolveDeviceZenModesSupport(context) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { RenderTopAppBar() },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!hasPermission) {
                    PermissionSection()
                } else {
                    SoundProfileSection(
                        scheduleExactAlarmsPermissionGranted = scheduleExactAlarmsPermissionGranted,
                        selectedMode = selectedMode,
                        iconTheme = iconTheme,
                        onSelectedMode = onSelectedMode,
                        onStartTimer = onStartTimer,
                        onCancelTimer = onCancelTimer,
                    )

                    QuickSettingsSection(iconTheme = iconTheme)
                }

                SilentModeSettingsSection(zenModeSupport, hasPermission)

                Spacer(Modifier.height(16.dp))

                IconThemeSelectionSection(
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
private fun PermissionSection() {
    RenderGrantPermissionCard()
}

@Composable
private fun SoundProfileSection(
    scheduleExactAlarmsPermissionGranted: Boolean,
    selectedMode: Int,
    iconTheme: IconTheme,
    onSelectedMode: (Int) -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
) {
    RenderNewCategoryName(
        title = stringResource(R.string.category_sound_mode_title),
        description = null,
    )
    RenderSoundModeSelectionCard(
        scheduleExactAlarmsPermissionGranted = scheduleExactAlarmsPermissionGranted,
        selectedMode = selectedMode,
        iconTheme = iconTheme,
        onSelectedMode = onSelectedMode,
        onStartTimer = onStartTimer,
        onCancelTimer = onCancelTimer,
    )
}

@Composable
private fun QuickSettingsSection(iconTheme: IconTheme) {
    val dataStoreManager = rememberDataStoreManager()
    val scope = rememberCoroutineScope()
    // Start with true so the "add tile" card doesn't flicker in before the stored value is loaded
    val tileAdded by dataStoreManager.tileAdded.collectAsState(initial = true)
    val showTimerEndTime by dataStoreManager.showTimerEndTime.collectAsState(initial = false)

    // This section is only shown with permission, so the add tile card only depends on the tile state.
    val showAddTileCard = !tileAdded
    // The end time setting only affects the tile subtitle, which exists on Android 10 and above.
    // Below that, the section is only needed for the add tile card.
    val showEndTimeSetting = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    if (!showAddTileCard && !showEndTimeSetting) return

    RenderNewCategoryName(
        title = stringResource(R.string.section_quick_access_title),
        description = null,
    )
    if (showAddTileCard) {
        RenderAddTileToStatusBarCard(
            iconTheme = iconTheme,
            onTileAdded = { scope.launch { dataStoreManager.setTileAdded(true) } },
        )
    }

    if (showEndTimeSetting) {
        SectionCard {
            RenderSettingsSwitchRow(
                title = stringResource(R.string.setting_show_end_time_title),
                description = stringResource(R.string.setting_show_end_time_description),
                checked = showTimerEndTime,
                onCheckedChange = { show -> scope.launch { dataStoreManager.setShowTimerEndTime(show) } },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SilentModeSettingsSection(
    zenModes: ZenModesSupport,
    hasPermission: Boolean,
) {
    // only render this section on Android 10 and above as the ZenMode policy is available on these versions onwards
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

    RenderNewCategoryName(
        title = stringResource(R.string.category_silent_mode_title),
        description = stringResource(R.string.category_silent_mode_description),
    )

    // versions below Android 15 only show notice card for ZenMode / ZenRule settings but customization is not possible on these versions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        ZenModesSettings(zenModes, hasPermission)
    } else {
        RenderZenModeSettingsNoticeCard()
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
private fun ZenModesSettings(
    zenModes: ZenModesSupport,
    hasPermission: Boolean,
) {
    when (zenModes) {
        // with userManaged modes, the user can press the button to open the ZenMode settings directly
        ZenModesSupport.UserManaged -> {
            val dataStoreManager = rememberDataStoreManager()
            val ruleId by dataStoreManager.zenRuleId.collectAsState(initial = "")
            RenderZenModeSettingsNoticeCard(
                actions = { RenderOpenZenModeSettings(ruleId, hasPermission) },
            )
        }

        // if no userManged modes are available on Android 15, show only the notice card and additionally the limitation card
        else -> {
            RenderZenModeSettingsNoticeCard()
            RenderNoUserManagedModesAvailableCard()
        }
    }
}

@Composable
private fun IconThemeSelectionSection(
    iconThemeIndex: Int,
    onIconThemeChangeIndex: (Int) -> Unit,
    hasPermission: Boolean,
) {
    RenderNewCategoryName(
        title = stringResource(R.string.category_icon_design_title),
        description = stringResource(R.string.category_icon_design_description),
    )

    RenderIconThemeSelector(
        selectedOptionIndex = iconThemeIndex,
        onOptionSelectedIndex = onIconThemeChangeIndex,
        hasPermission = hasPermission
    )
}
