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
import com.curryplayer.quicksettingssoundprofile.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SoundProfileTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var dataStoreManager: DataStoreManager

    /**
     * A [BroadcastReceiver] that listens for changes in the device's ringer mode and DnD
     * interruption filter. When a change is detected (e.g., 'Sound' -> 'Vibrate',
     * 'Vibrate' -> 'Silent', or a DnD filter change), it triggers an update to the Quick Settings
     * tile to reflect the new state. This ensures the tile is always in sync with the actual
     * system sound profile, including changes made via Android's native switches.
     */
    private val ringerModeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
                intent?.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
                updateTileState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        dataStoreManager = DataStoreManager(this)
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
        registerReceiver(ringerModeChangedReceiver, filter)
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
        serviceScope.cancel()
    }

    private fun unregisterRingerModeChangedReceiver() {
        try {
            unregisterReceiver(ringerModeChangedReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver previously not registered or already unregistered
        }
    }

    private fun changeSoundProfileAndUpdateTileState() {

        // disable tile if user has no permission to change sound profile
        if (!Utils.isDoNotDisturbPermissionGranted(this)) {
            if (qsTile != null) {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_warning_24)
                qsTile.state = Tile.STATE_UNAVAILABLE
                qsTile.label = getString(R.string.permission_required)
                qsTile.updateTile()
            }
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                // >= Android 15 should use an AutomaticZenRule for more control
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
            AudioManager.RINGER_MODE_SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
        updateTileState()
    }

    private fun updateTileState() {

        if (qsTile == null) {
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = getString(R.string.profile_sound_label)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_volume_up_24)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.profile_vibrate_label)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_vibration_24)
            }
            AudioManager.RINGER_MODE_SILENT -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.profile_silent_label)
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_volume_off_24)
            }

        }
        qsTile.updateTile()
    }

}
