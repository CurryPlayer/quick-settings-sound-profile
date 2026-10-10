package com.curryplayer.quicksettingssoundprofile

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.manager.SoundProfileManager
import com.curryplayer.quicksettingssoundprofile.models.AlarmItem
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption
import com.curryplayer.quicksettingssoundprofile.pages.RenderSettingsPage
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmScheduler
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.AlarmExactUtils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var soundProfileManager: SoundProfileManager
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var alarmScheduler: AlarmScheduler

    private var dndPermissionGrantedState by mutableStateOf(false)
    private var scheduleExactAlarmsPermissionGrantedState by mutableStateOf(false)
    private var selectedSoundModeState by mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL)

    /**
     * Holds the user-selected target ringer mode while an asynchronous mode transition is in flight.
     * Used to filter out transient system broadcasts (e.g. temporary switch to NORMAL when entering or leaving SILENT)
     * so the UI does not flicker between states.
     */
    private var pendingTargetMode: Int? = null

    /**
     * Tracks the active coroutine job performing the sound mode change.
     * Allows cancelling prior operations when the user rapidly switches modes.
     */
    private var modeChangeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundProfileManager = SoundProfileManager(this)
        dataStoreManager = soundProfileManager.dataStoreManager
        alarmScheduler = soundProfileManager.alarmScheduler
        selectedSoundModeState = soundProfileManager.currentRingerMode

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                soundProfileManager.ringerMode.collect { mode ->
                    val target = pendingTargetMode
                    if (target == null) {
                        selectedSoundModeState = mode
                    } else if (mode == target) {
                        pendingTargetMode = null
                        selectedSoundModeState = mode
                    }
                    // Transient broadcasts differing from pendingTargetMode are ignored
                }
            }
        }

        setContent {
            QuickSettingsSoundProfileTheme {
                val savedZenRuleId by dataStoreManager.zenRuleId.collectAsState(initial = "")
                val savedIconThemeIndex by dataStoreManager.iconTheme.collectAsState(initial = IconTheme.VOLUME_DEFAULT.ordinal)
                val timerEndTime by dataStoreManager.timerEndTime.collectAsState(initial = 0L)
                val previousRingerMode by dataStoreManager.previousRingerMode.collectAsState(initial = AudioManager.RINGER_MODE_NORMAL)
                val savedMuteDurationMinutes by dataStoreManager.lastMuteDurationMinutes.collectAsState(initial = MuteDurationOption.MINUTES_60.minutes)
                val scope = rememberCoroutineScope()

                RenderSettingsPage(
                    ctx = this,
                    hasPermission = dndPermissionGrantedState,
                    ruleId = savedZenRuleId,
                    iconThemeIndex = savedIconThemeIndex,
                    onIconThemeChangeIndex = { newIndexValue ->
                        scope.launch {
                            dataStoreManager.setIconTheme(newIndexValue)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && savedZenRuleId.isNotEmpty()) {
                                ZenRuleUtils.updateZenRuleIcon(
                                    context = this@MainActivity,
                                    ruleId = savedZenRuleId,
                                    iconResId = IconTheme.fromOrdinal(newIndexValue).silentIcon
                                )
                            }
                        }
                    },
                    scheduleExactAlarmsPermissionGranted = scheduleExactAlarmsPermissionGrantedState,
                    selectedMode = selectedSoundModeState,
                    onSelectedMode = { mode ->
                        // Optimistically update UI immediately and set pending target to filter intermediate system broadcasts
                        selectedSoundModeState = mode
                        pendingTargetMode = mode

                        // Cancel any previously running mode change job to avoid concurrent executions on rapid taps
                        modeChangeJob?.cancel()
                        val job = lifecycleScope.launch {
                            try {
                                soundProfileManager.setSoundMode(mode)
                            } finally {
                                // Only clean up and sync if this coroutine is still the latest active job
                                if (modeChangeJob == coroutineContext[Job]) {
                                    pendingTargetMode = null
                                    selectedSoundModeState = soundProfileManager.currentRingerMode
                                }
                            }
                        }
                        modeChangeJob = job
                    },
                    previousRingerMode = previousRingerMode,
                    timerEndTime = timerEndTime,
                    savedMuteDurationMinutes = savedMuteDurationMinutes,
                    onSaveMuteDuration = { minutes ->
                        scope.launch {
                            dataStoreManager.saveMuteDurationMinutes(minutes)
                        }
                    },
                    onStartTimer = { minutes ->
                        scope.launch {
                            alarmScheduler.schedule(AlarmItem(minutes))
                        }
                    },
                    onCancelTimer = {
                        scope.launch {
                            alarmScheduler.cancel()
                        }
                    }
                )

            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndSyncRule()
    }

    private fun checkPermissionsAndSyncRule() {
        dndPermissionGrantedState = soundProfileManager.canChangeSoundProfile()
        if (dndPermissionGrantedState) {
            lifecycleScope.launch {
                soundProfileManager.resolveZenRuleId()
            }
        }
        checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission() {
        scheduleExactAlarmsPermissionGrantedState = AlarmExactUtils.isScheduleExactAlarmsPermissionGranted(this)
    }
}
