package com.curryplayer.quicksettingssoundprofile.services

import android.media.AudioManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.curryplayer.quicksettingssoundprofile.utils.Utils

class SoundProfileTileService: TileService() {

    // Called when the user adds your tile.
    override fun onTileAdded() {
        super.onTileAdded()
        Log.i("SoundProfileService", "Tile added")
        updateSoundTile()
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
        Log.i("SoundProfileService", "Tile start listening")

    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
        Log.i("SoundProfileService", "Tile stop listening")
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        Log.i("SoundProfileService", "Tile clicked")
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        Log.i("Current Sound Mode", currentMode.toString())
        updateSoundTile()
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.i("SoundProfileService", "Tile removed")
    }

    private fun updateSoundTile() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        Log.i("Current Sound Mode", currentMode.toString())

        val isQsTileNull = (qsTile == null)
        Log.i("State of qsTile", isQsTileNull.toString())

        if (qsTile == null) {
            return
        }

        // in case the user has not granted the permission
        if (!Utils.isDoNotDisturbPermissionGranted(this)) {
            Log.i("Permission", "Not allowed to toggle sound profile")
            // qsTile.state = Tile.STATE_UNAVAILABLE
            // qsTile.label = "Missing Permission"
            // qsTile.updateTile()
            return
        }

        when(currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                qsTile.label = "Vibrate"
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                qsTile.label = "Silent"
            }
            AudioManager.RINGER_MODE_SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                qsTile.label = "Normal"
            }

        }
        qsTile.updateTile()
    }

}