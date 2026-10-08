package com.curryplayer.quicksettingssoundprofile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.curryplayer.quicksettingssoundprofile.composables.AppButton
import com.curryplayer.quicksettingssoundprofile.composables.AppButtonType
import com.curryplayer.quicksettingssoundprofile.composables.RenderSoundModeSelectionCard
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.manager.SoundProfileManager
import com.curryplayer.quicksettingssoundprofile.models.AlarmItem
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmScheduler
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.AlarmExactUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TilePreferencesActivity : ComponentActivity() {
    companion object {
        private const val DURATION_60_MINUTES = 60
    }

    private lateinit var _soundProfileManager: SoundProfileManager
    private lateinit var _dataStoreManager: DataStoreManager
    private lateinit var _alarmScheduler: AlarmScheduler

    private var _scheduleExactAlarmsPermissionGrantedState by mutableStateOf(false)
    private var _dndPermissionGrantedState by mutableStateOf(false)
    private var _selectedSoundModeState by mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL)
    /**
     * Holds the user-selected target ringer mode while an asynchronous mode transition is in flight.
     * Used to filter out transient system broadcasts (e.g. temporary switch to NORMAL when entering or leaving SILENT)
     * so the UI does not flicker between states.
     */
    private var _pendingTargetMode: Int? = null

    /**
     * Tracks the active coroutine job performing the sound mode change.
     * Allows cancelling prior operations when the user rapidly switches modes.
     */
    private var _modeChangeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _soundProfileManager = SoundProfileManager(this)
        _dataStoreManager = _soundProfileManager.dataStoreManager
        _alarmScheduler = _soundProfileManager.alarmScheduler
        _selectedSoundModeState = _soundProfileManager.currentRingerMode

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                _soundProfileManager.ringerMode.collect { mode ->
                    val target = _pendingTargetMode
                    if (target == null) {
                        _selectedSoundModeState = mode
                    } else if (mode == target) {
                        _pendingTargetMode = null
                        _selectedSoundModeState = mode
                    }
                    // Transient broadcasts differing from _pendingTargetMode are ignored
                }
            }
        }

        setContent {
            QuickSettingsSoundProfileTheme {
                val timerEndTime by _dataStoreManager.timerEndTime.collectAsState(initial = 0L)
                val previousRingerMode by _dataStoreManager.previousRingerMode.collectAsState(initial = AudioManager.RINGER_MODE_NORMAL)
                val savedIconThemeIndex by _dataStoreManager.iconTheme.collectAsState(initial = IconTheme.VOLUME_DEFAULT.ordinal)
                val savedMuteDurationMinutes by _dataStoreManager.lastMuteDurationMinutes.collectAsState(initial = DURATION_60_MINUTES)
                val iconTheme = remember(savedIconThemeIndex) { IconTheme.fromOrdinal(savedIconThemeIndex) }
                val scope = rememberCoroutineScope()

                RenderFloatingAlertActivity(
                    selectedMode = _selectedSoundModeState,
                    timerEndTime = timerEndTime,
                    previousRingerMode = previousRingerMode,
                    savedMuteDurationMinutes = savedMuteDurationMinutes,
                    scope = scope,
                    iconTheme = iconTheme
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndSyncRule()
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing) {
            finish()
        }
    }

    private fun checkPermissionsAndSyncRule() {
        _dndPermissionGrantedState = _soundProfileManager.canChangeSoundProfile()
        if (_dndPermissionGrantedState) {
            lifecycleScope.launch {
                _soundProfileManager.resolveZenRuleId()
            }
        }
        checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission() {
        _scheduleExactAlarmsPermissionGrantedState = AlarmExactUtils.isScheduleExactAlarmsPermissionGranted(this)
    }

    @Composable
    private fun RenderFloatingAlertActivity(
        selectedMode: Int,
        timerEndTime: Long,
        previousRingerMode: Int,
        savedMuteDurationMinutes: Int,
        scope: CoroutineScope,
        iconTheme: IconTheme
    ) {
        AlertDialog(
            onDismissRequest = { finish() },
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = AlertDialogDefaults.shape
            ),
            title = {
                Text(text = stringResource(R.string.timer_title))
            },
            text = {
                RenderSoundModeSelectionCard(
                    ctx = this@TilePreferencesActivity,
                    dndPermissionGranted = _dndPermissionGrantedState,
                    scheduleExactAlarmsPermissionGranted = _scheduleExactAlarmsPermissionGrantedState,
                    selectedMode = selectedMode,
                    iconTheme = iconTheme,
                    onSelectedMode = { mode ->
                        // Optimistically update UI immediately and set pending target to filter intermediate system broadcasts
                        _selectedSoundModeState = mode
                        _pendingTargetMode = mode

                        // Cancel any previously running mode change job to avoid concurrent executions on rapid taps
                        _modeChangeJob?.cancel()
                        val job = lifecycleScope.launch {
                            try {
                                applyModeImmediately(mode)
                            } finally {
                                // Only clean up and sync if this coroutine is still the latest active job
                                if (_modeChangeJob == coroutineContext[Job]) {
                                    _pendingTargetMode = null
                                    _selectedSoundModeState = _soundProfileManager.currentRingerMode
                                }
                            }
                        }
                        _modeChangeJob = job
                    },
                    previousRingerMode = previousRingerMode,
                    timerEndTime = timerEndTime,
                    savedMuteDurationMinutes = savedMuteDurationMinutes,
                    onSaveMuteDuration = { minutes ->
                        scope.launch {
                            _dataStoreManager.saveMuteDurationMinutes(minutes)
                        }
                    },
                    onStartTimer = { minutes ->
                        scope.launch {
                            _alarmScheduler.schedule(AlarmItem(minutes))
                        }
                    },
                    onCancelTimer = {
                        scope.launch {
                            _alarmScheduler.cancel()
                        }
                    }
                )
            },
            confirmButton = {
                RenderAlertDialogButtons()
            },
            dismissButton = {
                // unused
            }
        )
    }

    @Composable
    private fun RenderAlertDialogButtons() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppButton(
                text = stringResource(R.string.open_app_button),
                onClick = {
                    val intent = Intent(
                        this@TilePreferencesActivity,
                        MainActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    try {
                        startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            this@TilePreferencesActivity,
                            this@TilePreferencesActivity.getString(R.string.toast_intent_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    finish()
                },
                type = AppButtonType.OUTLINED
            )
            AppButton(
                text = stringResource(R.string.alert_button_close),
                onClick = { finish() },
                type = AppButtonType.FILLED
            )
        }
    }

    private suspend fun applyModeImmediately(targetMode: Int) {
        _soundProfileManager.setSoundMode(targetMode)
    }
}
