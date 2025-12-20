package com.curryplayer.quicksettingssoundprofile.services

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
import com.curryplayer.quicksettingssoundprofile.utils.Utils

class SoundProfileTileService: TileService(){

    /**
     * A [BroadcastReceiver] that listens for changes in the device's ringer mode.
     * When a change is detected (e.g., 'Sound' -> 'Vibrate' or 'Vibrate' -> 'Silent'), it triggers
     * an update to the Quick Settings tile to reflect the new state. This ensures the tile
     * is always in sync with the actual system sound profile.
     */
    private val ringerModeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                Log.i("SoundProfileTileService", "Ringer mode changed event received by inner receiver. Updating tile.")
                updateTileState()
            }
        }

    }

    // Called when the user adds your tile.
    override fun onTileAdded() {
        super.onTileAdded()
        Log.i("SoundProfileService", "Tile added")
        updateTileState()
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.i("SoundProfileService", "Tile removed")

        if (qsTile == null) {
            return
        }

        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
        Log.i("SoundProfileService", "Tile start listening")
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(ringerModeChangedReceiver, filter)
        updateTileState()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
        Log.i("SoundProfileService", "Tile stop listening")
        unregisterRingerModeChangedReceiver()
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        Log.i("SoundProfileService", "Tile clicked")
        changeSoundProfileAndUpdateTileState()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("SoundProfileService", "onDestroy was called")
        unregisterRingerModeChangedReceiver()
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
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            AudioManager.RINGER_MODE_SILENT -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        updateTileState()
    }

    /**
     * Updates the sound profile tile.
     *
     */
    private fun updateTileState() {

        if (qsTile == null) {
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        Log.i("Current Sound Mode", currentMode.toString())

        val isQsTileNull = (qsTile == null)
        Log.i("State of qsTile", isQsTileNull.toString())

        when(currentMode) {
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