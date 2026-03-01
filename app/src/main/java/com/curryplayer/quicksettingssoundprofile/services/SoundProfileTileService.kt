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
import android.util.Log
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SoundProfileTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var dataStoreManager: DataStoreManager

    /**
     * A [BroadcastReceiver] that listens for changes in the device's ringer mode.
     * When a change is detected (e.g., 'Sound' -> 'Vibrate' or 'Vibrate' -> 'Silent'), it triggers
     * an update to the Quick Settings tile to reflect the new state. This ensures the tile
     * is always in sync with the actual system sound profile.
     */
    private val ringerModeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                Log.i("SoundProfileTileService", "Ringer mode changed event received. Updating tile.")
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
        Log.i("SoundProfileService", "Tile added")
        updateTileState()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.i("SoundProfileService", "Tile removed")
        if (qsTile != null) {
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.i("SoundProfileService", "Tile start listening")
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(ringerModeChangedReceiver, filter)
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.i("SoundProfileService", "Tile stop listening")
        unregisterRingerModeChangedReceiver()
    }

    override fun onClick() {
        super.onClick()
        Log.i("SoundProfileService", "Tile clicked")
        changeSoundProfileAndUpdateTileState()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("SoundProfileService", "onDestroy was called")
        unregisterRingerModeChangedReceiver()
        serviceScope.cancel()
    }

    private fun unregisterRingerModeChangedReceiver() {
        try {
            unregisterReceiver(ringerModeChangedReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver previously not registered or already unregistered
            Log.i("SoundProfileTileService", e.toString())
        }
    }

    private fun changeSoundProfileAndUpdateTileState() {

        // disable tile if user has no permission to change sound profile
        if (!Utils.isDoNotDisturbPermissionGranted(this)) {
            Log.i("SoundProfileTileService", "Not allowed to toggle sound profile")
            if (qsTile != null) {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_warning_24)
                qsTile.state = Tile.STATE_UNAVAILABLE
                qsTile.label = "Permission required"
                qsTile.updateTile()
            }
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Use runBlocking to get the settings synchronously for the immediate action
        val shouldActivateDnd = runBlocking { dataStoreManager.activateDnd.first() }
        val shouldMuteMedia = runBlocking { dataStoreManager.muteMedia.first() }
        val lastVolumeLevel = runBlocking { dataStoreManager.volumeLevel.first() }

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                if (shouldActivateDnd) {
                    Log.i("RingerMode", "DND is set to active for switching to silent mode")
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                }
                if (shouldMuteMedia) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    serviceScope.launch {
                        dataStoreManager.setVolumeLevel(lastVolumeLevel)
                    }
                }
            }
            AudioManager.RINGER_MODE_SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                if (shouldActivateDnd) {
                    Log.i("RingerMode", "DND is set to active for switching to normal mode")
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                if (shouldMuteMedia) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolumeLevel, 0)
                }
            }
        }
        updateTileState()
    }

    private fun updateTileState() {

        if (qsTile == null) {
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        Log.i("Current Sound Mode", currentMode.toString())

        val isQsTileNull = (qsTile == null)
        Log.i("State of qsTile", isQsTileNull.toString())

        when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = "Sound"
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_volume_up_24)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = "Vibrate"
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_vibration_24)
            }
            AudioManager.RINGER_MODE_SILENT -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = "Silent"
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_volume_off_24)
            }

        }
        qsTile.updateTile()
    }

}
