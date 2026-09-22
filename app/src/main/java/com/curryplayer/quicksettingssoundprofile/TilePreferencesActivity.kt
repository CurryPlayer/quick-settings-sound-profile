package com.curryplayer.quicksettingssoundprofile

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.curryplayer.quicksettingssoundprofile.composables.AppButton
import com.curryplayer.quicksettingssoundprofile.composables.AppButtonType
import com.curryplayer.quicksettingssoundprofile.composables.RenderGrantPermissionCard
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.models.AlarmItem
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.receivers.RingerModeReceiver
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmScheduler
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmSchedulerImpl
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.AlarmExactUtils
import com.curryplayer.quicksettingssoundprofile.utils.NotificationPolicyUtils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class TilePreferencesActivity : ComponentActivity() {
    companion object {
        private const val DURATION_30_MINUTES = 30
        private const val DURATION_60_MINUTES = 60
        private const val DURATION_180_MINUTES = 180
    }

    private lateinit var _dataStoreManager: DataStoreManager
    private lateinit var _alarmScheduler: AlarmScheduler
    private lateinit var _ringerModeReceiver: RingerModeReceiver

    private var _scheduleExactAlarmsPermissionGrantedState by mutableStateOf(false)
    private var _dndPermissionGrantedState by mutableStateOf(false)
    private var _selectedSoundModeState by mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _dataStoreManager = DataStoreManager(this)
        _alarmScheduler = AlarmSchedulerImpl(this, _dataStoreManager)
        _ringerModeReceiver = RingerModeReceiver(this) {
            updateRingerModeState()
        }
        updateRingerModeState()

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
        updateRingerModeState()
        _ringerModeReceiver.register()
    }

    override fun onPause() {
        super.onPause()
        _ringerModeReceiver.unregister()
        if (!isFinishing) {
            finish()
        }
    }

    private fun updateRingerModeState() {
        _selectedSoundModeState = getSystemService(AudioManager::class.java).ringerMode
    }

    private fun checkPermissionsAndSyncRule() {
        _dndPermissionGrantedState = NotificationPolicyUtils.isDoNotDisturbPermissionGranted(this)
        if (_dndPermissionGrantedState) {
            lifecycleScope.launch {
                ZenRuleUtils.syncAutomaticZenRule(this@TilePreferencesActivity, _dataStoreManager)
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
                var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

                LaunchedEffect(timerEndTime) {
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        if (timerEndTime <= now) break
                        currentTime = now

                        val remainingMillis = timerEndTime - now
                        val millisUntilNextMinute = remainingMillis % 60_000L
                        val nextDelay = if (millisUntilNextMinute == 0L) 60_000L else millisUntilNextMinute
                        delay(nextDelay.milliseconds)
                    }
                }

                val isTimerActive = timerEndTime > currentTime

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (!_dndPermissionGrantedState) {
                        RenderGrantPermissionCard(ctx = this@TilePreferencesActivity, outerPadding = 0)
                    } else {

                        RenderGrantExactAlarmPermission(_scheduleExactAlarmsPermissionGrantedState)

                        RenderSoundModeSelection(
                            selectedMode = selectedMode,
                            iconTheme = iconTheme,
                            onSelectedMode = { mode ->
                                _selectedSoundModeState = mode
                            }
                        )

                        RenderTemporaryMuteSelection(
                            selectedMode = selectedMode,
                            previousRingerMode = previousRingerMode,
                            isTimerActive = isTimerActive,
                            timerEndTime = timerEndTime,
                            currentTime = currentTime,
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
                    }
                }

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
    private fun RenderGrantExactAlarmPermission(
        canScheduleExactAlarms: Boolean
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!canScheduleExactAlarms) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_round_info_24),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.grant_exact_alarm_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.grant_exact_alarm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    openExactAlarmSettings(this@TilePreferencesActivity)
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.button_grant_exact_alarm),
                                        style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                    }
                }
            }
        }
    }

    @Composable
    private fun RenderSoundModeSelection(
        selectedMode: Int,
        iconTheme: IconTheme,
        onSelectedMode: (Int) -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.sound_mode_selection_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sound Mode Card
                SoundModeCard(
                    title = stringResource(R.string.profile_sound_label),
                    iconRes = iconTheme.ringIcon,
                    isSelected = selectedMode == AudioManager.RINGER_MODE_NORMAL,
                    onClick = {
                        onSelectedMode(AudioManager.RINGER_MODE_NORMAL)
                        lifecycleScope.launch {
                            applyModeImmediately(AudioManager.RINGER_MODE_NORMAL)
                        }

                    },
                    modifier = Modifier.weight(1f)
                )

                // Vibrate Mode Card
                SoundModeCard(
                    title = stringResource(R.string.profile_vibrate_label),
                    iconRes = iconTheme.vibrateIcon,
                    isSelected = selectedMode == AudioManager.RINGER_MODE_VIBRATE,
                    onClick = {
                        onSelectedMode(AudioManager.RINGER_MODE_VIBRATE)
                        lifecycleScope.launch {
                            applyModeImmediately(AudioManager.RINGER_MODE_VIBRATE)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Mute / Silent Mode Card
                SoundModeCard(
                    title = stringResource(R.string.profile_silent_label),
                    iconRes = iconTheme.silentIcon,
                    isSelected = selectedMode == AudioManager.RINGER_MODE_SILENT,
                    onClick = {
                        onSelectedMode(AudioManager.RINGER_MODE_SILENT)
                        lifecycleScope.launch {
                            applyModeImmediately(AudioManager.RINGER_MODE_SILENT)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RenderTemporaryMuteSelection(
        selectedMode: Int,
        previousRingerMode: Int,
        isTimerActive: Boolean,
        timerEndTime: Long,
        currentTime: Long,
        savedMuteDurationMinutes: Int,
        onSaveMuteDuration: (minutes: Int) -> Unit,
        onStartTimer: (minutes: Int) -> Unit,
        onCancelTimer: () -> Unit,
    ) {
        // FIXME: This will result in one of these three options is selected when custom time has the same value
        var isCustomSelected by remember(savedMuteDurationMinutes) {
            mutableStateOf(savedMuteDurationMinutes !in listOf(DURATION_30_MINUTES, DURATION_60_MINUTES, DURATION_180_MINUTES))
        }
        var selectedPresetMinutes by remember(savedMuteDurationMinutes) {
            mutableIntStateOf(if (savedMuteDurationMinutes in listOf(DURATION_30_MINUTES, DURATION_60_MINUTES, DURATION_180_MINUTES)) savedMuteDurationMinutes else DURATION_60_MINUTES)
        }
        var customHours by remember(savedMuteDurationMinutes) {
            mutableIntStateOf(savedMuteDurationMinutes / DURATION_60_MINUTES)
        }
        var customMinutes by remember(savedMuteDurationMinutes) {
            mutableIntStateOf(savedMuteDurationMinutes % DURATION_60_MINUTES)
        }
        var showTimeSelectorDialog by remember { mutableStateOf(false) }

        val finalMinutes = if (isCustomSelected) {
            customHours * DURATION_60_MINUTES + customMinutes
        } else {
            selectedPresetMinutes
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            if (selectedMode == AudioManager.RINGER_MODE_SILENT) {

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.temporary_mute_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val targetModeLabel = when (previousRingerMode) {
                            AudioManager.RINGER_MODE_VIBRATE -> stringResource(R.string.profile_vibrate_label)
                            else -> stringResource(R.string.profile_sound_label)
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.temporary_mute_desc, targetModeLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isTimerActive) {
                            Spacer(modifier = Modifier.height(16.dp))

                            val remainingMillis = (timerEndTime - currentTime).coerceAtLeast(0L)
                            val totalMinutes = (remainingMillis + 59_999L) / 60_000L
                            val hours = totalMinutes / 60
                            val minutes = totalMinutes % 60

                            val timeString = when {
                                hours > 0 && minutes > 0 -> stringResource(R.string.time_format_hours_minutes, hours, minutes)
                                hours > 0 -> stringResource(R.string.time_format_hours_only, hours)
                                else -> stringResource(R.string.time_format_minutes_only, minutes)
                            }

                            Text(
                                text = stringResource(R.string.time_remaining, timeString),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        }
                    }

                    VerticalDivider(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 2.dp
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Switch(
                        checked = isTimerActive,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                onSaveMuteDuration(finalMinutes)
                                onStartTimer(finalMinutes)
                            } else {
                                onCancelTimer()
                            }
                        }
                    )
                }

                if (isTimerActive) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = stringResource(R.string.select_duration),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FilterChip(
                            selected = !isCustomSelected && selectedPresetMinutes == DURATION_30_MINUTES,
                            onClick = {
                                isCustomSelected = false
                                selectedPresetMinutes = DURATION_30_MINUTES
                                onSaveMuteDuration(DURATION_30_MINUTES)
                                if (isTimerActive) {
                                    onStartTimer(DURATION_30_MINUTES)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.duration_30m),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                        FilterChip(
                            selected = !isCustomSelected && selectedPresetMinutes == DURATION_60_MINUTES,
                            onClick = {
                                isCustomSelected = false
                                selectedPresetMinutes = DURATION_60_MINUTES
                                onSaveMuteDuration(DURATION_60_MINUTES)
                                if (isTimerActive) {
                                    onStartTimer(DURATION_60_MINUTES)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.duration_1h),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                        FilterChip(
                            selected = !isCustomSelected && selectedPresetMinutes == DURATION_180_MINUTES,
                            onClick = {
                                isCustomSelected = false
                                selectedPresetMinutes = DURATION_180_MINUTES
                                onSaveMuteDuration(DURATION_180_MINUTES)
                                if (isTimerActive) {
                                    onStartTimer(DURATION_180_MINUTES)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.duration_3h),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isCustomSelected,
                            onClick = {
                                isCustomSelected = true
                                showTimeSelectorDialog = true
                                if (isTimerActive) {
                                    onStartTimer(customHours * DURATION_60_MINUTES + customMinutes)
                                }
                            },
                            label = {
                                // TODO: Fix text representation for different translations (min, m, h, Std....)
                                val customText = if (isCustomSelected && finalMinutes > 0) {
                                    "${stringResource(R.string.custom_minutes)} (${finalMinutes}m)"
                                } else {
                                    stringResource(R.string.custom_minutes)
                                }
                                Text(
                                    text = customText,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }

                    if (showTimeSelectorDialog) {
                        val timePickerState = rememberTimePickerState(
                            initialHour = customHours,
                            initialMinute = customMinutes,
                            is24Hour = true
                        )

                        TimePickerDialog(
                            onDismissRequest = { showTimeSelectorDialog = false },
                            title = {
                                Text(
                                    // TODO: translate text
                                    text = "Dauer einstellen",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            confirmButton = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AppButton(
                                        text = stringResource(R.string.alert_button_close),
                                        onClick = {
                                            showTimeSelectorDialog = false
                                        },
                                        type = AppButtonType.OUTLINED
                                    )
                                    AppButton(
                                        text = stringResource(R.string.start_timer),
                                        onClick = {
                                            customHours = timePickerState.hour
                                            customMinutes = timePickerState.minute
                                            val duration = timePickerState.hour * DURATION_60_MINUTES + timePickerState.minute
                                            onSaveMuteDuration(duration)
                                            if (isTimerActive) {
                                                onStartTimer(duration)
                                            }
                                            showTimeSelectorDialog = false
                                        },
                                        type = AppButtonType.FILLED
                                    )
                                }
                            },
                            dismissButton = {
                                // unused
                            }
                        ) {
                            TimePicker(
                                state = timePickerState
                            )
                        }
                    }
                }

            }
        }
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun openExactAlarmSettings(ctx: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            "package:$packageName".toUri()
        )
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                ctx,
                ctx.getString(R.string.toast_intent_failed),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private suspend fun applyModeImmediately(targetMode: Int) {
        val currentMode = getSystemService(AudioManager::class.java).ringerMode
        if (targetMode == AudioManager.RINGER_MODE_SILENT && currentMode != AudioManager.RINGER_MODE_SILENT) {
            _dataStoreManager.setPreviousRingerMode(currentMode)
        } else if (targetMode != AudioManager.RINGER_MODE_SILENT) {
            _dataStoreManager.setPreviousRingerMode(targetMode)
        }

        val ruleId = resolveZenRuleId()
        val activate = (targetMode == AudioManager.RINGER_MODE_SILENT)
        ZenRuleUtils.applyZenRuleAndRingerMode(this, ruleId, activate, targetMode)

        _alarmScheduler.cancel()
    }

    @Composable
    private fun SoundModeCard(
        title: String,
        iconRes: Int,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
            }
        }
    }

    private suspend fun resolveZenRuleId(): String {
        val notificationManager = getSystemService(NotificationManager::class.java)
        var cachedId = _dataStoreManager.zenRuleId.first()
        if (cachedId.isEmpty() || notificationManager.getAutomaticZenRule(cachedId) == null) {
            cachedId = ZenRuleUtils.syncAutomaticZenRule(this@TilePreferencesActivity, _dataStoreManager)
        }
        return cachedId
    }
}
