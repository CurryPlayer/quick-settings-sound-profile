package com.curryplayer.quicksettingssoundprofile.services

import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.manager.SoundProfileManager
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SoundProfileTileService : TileService() {

    private val _serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var _listeningJob: Job? = null

    private lateinit var _soundProfileManager: SoundProfileManager
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
    private var _lastKnownTimerEndTime: Long = 0
    private var _timerEndTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        _soundProfileManager = SoundProfileManager(this)
        _dataStoreManager = _soundProfileManager.dataStoreManager

        _serviceScope.launch {
            _dataStoreManager.iconTheme.collect { themeId ->
                _iconTheme = IconTheme.fromOrdinal(themeId)
                updateTileState()
            }
        }
        _serviceScope.launch {
            _dataStoreManager.timerEndTime.collect { time ->
                _timerEndTime = time
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
        _listeningJob = _serviceScope.launch {
            _soundProfileManager.ringerMode.collect {
                updateTileState()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        _listeningJob?.cancel()
        _listeningJob = null
    }

    override fun onClick() {
        super.onClick()
        changeSoundProfileAndUpdateTileState()
    }

    override fun onDestroy() {
        super.onDestroy()
        _serviceScope.cancel()
    }

    private fun changeSoundProfileAndUpdateTileState() {
        // Disable tile if user has no permission to change sound profile
        if (!_soundProfileManager.canChangeSoundProfile()) {
            if (qsTile != null) {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_warning_24)
                qsTile.state = Tile.STATE_UNAVAILABLE
                qsTile.label = getString(R.string.permission_required)
                qsTile.updateTile()
            }
            _lastKnownRingerMode = -1 // otherwise the tile would stay in an unavailable state
            return
        }

        _serviceScope.launch {
            _soundProfileManager.cycleToNextMode()
            updateTileState()
        }
    }

    private fun updateTileState() {
        if (qsTile == null) {
            return
        }

        val currentMode = _soundProfileManager.currentRingerMode

        // Only update tile if the mode, theme or timerEndTime has actually changed since the last update
        if (currentMode == _lastKnownRingerMode && _iconTheme == _lastKnownIconTheme && _timerEndTime == _lastKnownTimerEndTime) {
            return
        }

        _lastKnownRingerMode = currentMode
        _lastKnownIconTheme = _iconTheme
        _lastKnownTimerEndTime = _timerEndTime

        when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = getString(R.string.profile_sound_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    qsTile.subtitle = null
                }
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.profile_vibrate_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    qsTile.subtitle = null
                }
            }
            AudioManager.RINGER_MODE_SILENT -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.profile_silent_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (_timerEndTime > System.currentTimeMillis()) {
                        val formattedLocalTime = Instant.ofEpochMilli(_timerEndTime)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                        qsTile.subtitle = getString(R.string.timer_active_until, formattedLocalTime)
                    } else {
                        qsTile.subtitle = null
                    }
                }
            }
        }

        qsTile.icon = Icon.createWithResource(this, _iconTheme.getIconForMode(currentMode))
        qsTile.updateTile()
    }
}
