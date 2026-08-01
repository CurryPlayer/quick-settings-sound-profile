package com.curryplayer.quicksettingssoundprofile.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.notification.Condition
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.utils.NotificationPolicyUtils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class SoundProfileTileService : TileService() {

    private val _serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var _dataStoreManager: DataStoreManager

    /**
     * Cache the last known ringer mode to avoid redundant tile updates.
     * SILENT = 0
     * VIBRATE = 1
     * NORMAL = 2
     */
    private var _lastKnownRingerMode: Int = -1
    private var _iconTheme: Int = 0
    private var _lastKnownIconTheme: Int = -1
    private var _cachedRuleId: String = ""

    /**
     * A [BroadcastReceiver] that listens for changes in the device's ringer mode and DnD
     * interruption filter. When a change is detected (e.g., 'Sound' -> 'Vibrate',
     * 'Vibrate' -> 'Silent', or a DnD filter change), it triggers an update to the Quick Settings
     * tile to reflect the new state. This ensures the tile is always in sync with the actual
     * system sound profile, including changes made via Android's native switches.
     */
    private val _ringerModeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
                intent?.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
                updateTileState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        _dataStoreManager = DataStoreManager(this)
        _serviceScope.launch {
            _dataStoreManager.zenRuleId.collect { id ->
                _cachedRuleId = id
            }
        }
        _serviceScope.launch {
            _dataStoreManager.iconTheme.collect { theme ->
                _iconTheme = theme
                //updateTileState()
            }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        if (qsTile != null) {
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION).apply {
            // it seems that an interruption filter also has an effect on the audioManager.ringerMode to change its behavior
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        registerReceiver(_ringerModeChangedReceiver, filter)
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterRingerModeChangedReceiver()
    }

    override fun onClick() {
        super.onClick()
        changeSoundProfileAndUpdateTileState()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterRingerModeChangedReceiver()
        _serviceScope.cancel()
    }

    private fun unregisterRingerModeChangedReceiver() {
        try {
            unregisterReceiver(_ringerModeChangedReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver previously not registered or already unregistered
        }
    }

    private fun changeSoundProfileAndUpdateTileState() {

        // disable tile if user has no permission to change sound profile
        if (!NotificationPolicyUtils.isDoNotDisturbPermissionGranted(this)) {
            if (qsTile != null) {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_warning_24)
                qsTile.state = Tile.STATE_UNAVAILABLE
                qsTile.label = getString(R.string.permission_required)
                qsTile.updateTile()
            }
            _lastKnownRingerMode = -1   // otherwise the tile would stay in an unavailable state
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        _serviceScope.launch {
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }

                AudioManager.RINGER_MODE_VIBRATE -> {
                    val ruleId = resolveZenRuleId()
                    setAutomaticZenRuleState(ruleId, true)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }

                AudioManager.RINGER_MODE_SILENT -> {
                    val ruleId = resolveZenRuleId()
                    setAutomaticZenRuleState(ruleId, false)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
            updateTileState()
        }

    }

    private suspend fun resolveZenRuleId(): String {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (_cachedRuleId.isEmpty() || notificationManager.getAutomaticZenRule(_cachedRuleId) == null) {
            // if the rule does not exist, create it and cache its ID
            _cachedRuleId = ZenRuleUtils.syncAutomaticZenRule(this@SoundProfileTileService, _dataStoreManager)
        }
        return _cachedRuleId
    }

    private fun updateTileState() {

        if (qsTile == null) {
            return
        }

        val currentMode = (getSystemService(AUDIO_SERVICE) as AudioManager).ringerMode

        // Only update tile if the mode or theme has actually changed since the last update
        if (currentMode == _lastKnownRingerMode && _iconTheme == _lastKnownIconTheme) {
            return
        }

        _lastKnownRingerMode = currentMode
        _lastKnownIconTheme = _iconTheme

        when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = getString(R.string.profile_sound_label)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.profile_vibrate_label)
            }
            AudioManager.RINGER_MODE_SILENT -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.profile_silent_label)
            }
        }

        qsTile.icon = Icon.createWithResource(this, getIconResource(currentMode))
        qsTile.updateTile()
    }

    private fun getIconResource(ringerMode: Int): Int {
        return when (_iconTheme) {
            1 -> { // Notifications Theme
                when (ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> R.drawable.ic_round_notifications_active_24
                    AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_round_vibration_outline_24
                    AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_round_notifications_off_24
                    else -> R.drawable.ic_round_notifications_active_24
                }
            }
            else -> { // Volume Theme (default)
                when (ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> R.drawable.ic_round_volume_up_24
                    AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_round_vibration_24
                    AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_round_volume_off_24
                    else -> R.drawable.ic_round_volume_up_24
                }
            }
        }
    }

    private fun setAutomaticZenRuleState(ruleId: String, activate: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val interruptionFilter = if (activate) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL

        if (ruleId.isNotEmpty()) {
            // requires Android 10 / API 29 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val conditionId = ZenRuleUtils.SILENT_CONDITION_DND_AND_MODE_URI.toUri()
                val summary = if (activate) "Active" else "Inactive"
                val state = if (activate) Condition.STATE_TRUE else Condition.STATE_FALSE
                val condition = ZenRuleUtils.buildCondition(conditionId, summary, state)
                notificationManager.setAutomaticZenRuleState(ruleId, condition)
            } else {
                // fallback for older Android versions
                // ensure the ZenRule is enabled/disabled
                val zenRule = notificationManager.getAutomaticZenRule(ruleId)
                if (zenRule != null && zenRule.isEnabled != activate) {
                    zenRule.isEnabled = activate
                    notificationManager.updateAutomaticZenRule(ruleId, zenRule)
                }
                // directly set the interruption filter as fallback
                notificationManager.setInterruptionFilter(interruptionFilter)
            }
        } else {
            // fallback to set the default interruption filter if rule does not exist
            notificationManager.setInterruptionFilter(interruptionFilter)
        }
    }

}
