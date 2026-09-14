package com.curryplayer.quicksettingssoundprofile.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.utils.NotificationPolicyUtils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
    private var _iconTheme: IconTheme = IconTheme.VOLUME_DEFAULT
    private var _lastKnownIconTheme: IconTheme? = null
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
            _dataStoreManager.iconTheme.collect { themeId ->
                _iconTheme = IconTheme.fromOrdinal(themeId)
                updateTileState()
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

        val audioManager = getSystemService(AudioManager::class.java)

        _serviceScope.launch {
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }

                AudioManager.RINGER_MODE_VIBRATE -> {
                    val ruleId = resolveZenRuleId()
                    ZenRuleUtils.applyZenRuleAndRingerMode(
                        this@SoundProfileTileService,
                        ruleId = ruleId,
                        activate = true,
                        targetRingerMode = AudioManager.RINGER_MODE_SILENT
                    )
                }

                AudioManager.RINGER_MODE_SILENT -> {
                    val ruleId = resolveZenRuleId()
                    ZenRuleUtils.applyZenRuleAndRingerMode(
                        this@SoundProfileTileService,
                        ruleId = ruleId,
                        activate = false,
                        targetRingerMode = AudioManager.RINGER_MODE_NORMAL
                    )
                }
            }
            updateTileState()
        }

    }

    private suspend fun resolveZenRuleId(): String {
        val notificationManager = getSystemService(NotificationManager::class.java)
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

        val currentMode = getSystemService(AudioManager::class.java).ringerMode

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

        qsTile.icon = Icon.createWithResource(this, _iconTheme.getIconForMode(currentMode))
        qsTile.updateTile()
    }

}
