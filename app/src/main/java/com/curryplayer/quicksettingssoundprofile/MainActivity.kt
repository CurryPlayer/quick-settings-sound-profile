package com.curryplayer.quicksettingssoundprofile

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.curryplayer.quicksettingssoundprofile.manager.SoundProfileManager
import com.curryplayer.quicksettingssoundprofile.models.AlarmItem
import com.curryplayer.quicksettingssoundprofile.pages.RenderMainPage
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmScheduler
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.PermissionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var soundProfileManager: SoundProfileManager
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
                val scope = rememberCoroutineScope()

                RenderMainPage(
                    hasPermission = dndPermissionGrantedState,
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
        scheduleExactAlarmsPermissionGrantedState = PermissionUtils.isScheduleExactAlarmsPermissionGranted(this)
    }
}
