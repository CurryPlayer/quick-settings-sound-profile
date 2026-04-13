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
import android.util.Log
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.utils.Utils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.core.net.toUri

class SoundProfileTileService : TileService() {

    // TODO:
    /*
    It seems that the Logic of switching between modes currently does not work properly. Multiple mode updates are sent.
     */

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

        val savedRuleId = runBlocking { dataStoreManager.zenRuleId.first() }
        Log.d("SoundProfileTileService", "Saved Rule ID: $savedRuleId")

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            AudioManager.RINGER_MODE_VIBRATE -> {

                // TODO: maybe update rule here if changes were made to it (or directly in MainActivity)
                // something like:
                /*
                val ruleIdToUse = if (savedRuleId.isNotEmpty()) {
                    notificationManager.updateAutomaticZenRule(savedRuleId, zenRule)
                    savedRuleId
                } else {
                    val newId = notificationManager.addAutomaticZenRule(zenRule)
                    onNewRuleId(newId) // Callback um ID im DataStore zu speichern
                    newId
                }
                 */

                val ruleId = if (!ZenRuleUtils.isAutomaticZenRuleRegistered(this, savedRuleId)) {
                    addAutomaticZenRule()
                } else {
                    savedRuleId
                }
                // The ZenRule must (presumably) be activated before setting the RingerMode
                activateAutomaticZenRule(ruleId)
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            AudioManager.RINGER_MODE_SILENT -> {
                // The ZenRule must (presumably) be deactivated before setting the RingerMode
                deactivateAutomaticZenRule(savedRuleId)
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
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

    /**
     * Adds an AutomaticZenRule to the device and returns the ruleId if successful.
     * If the ZenRule was successfully created, the rule ID will also be stored in DataStore.
     */
    private fun addAutomaticZenRule(): String {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val zenRule = ZenRuleUtils.generateDefaultAutomaticZenRule(this)
        val newRuleId = notificationManager.addAutomaticZenRule(zenRule)

        if (newRuleId != null) {
            Log.i("SoundProfileTileService", "New Rule created with ID: $newRuleId")
            activateAutomaticZenRule(newRuleId)
            serviceScope.launch {
                dataStoreManager.setZenRuleId(newRuleId)
            }
            return newRuleId
        } else {
            Log.e("SoundProfileTileService", "Failed to add Automatic Zen Rule.")
            return ""
        }
    }

    private fun activateAutomaticZenRule(ruleId: String) {
        /*
        The ZenRule already includes the "INTERRUPTION_FILTER_PRIORITY" interruption filter, which (most likely) automatically sets the ring mode to "SILENT".
        It is therefore (probably) not necessary to set the ringer mode manually on Android 10 and above. If it were set to “SILENT” again, this would simultaneously activate the ZenRule provided by this app and the default (already existing) Do Not Disturb mode.
         */
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (ruleId.isNotEmpty()) {
            // requires Android 10 / API 29 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val conditionId = ZenRuleUtils.SILENT_CONDITION_DND_AND_MODE_URI.toUri()
                val condition = Condition(conditionId, "Active", Condition.STATE_TRUE)
                notificationManager.setAutomaticZenRuleState(ruleId, condition)
            } else {
                // fallback for older Android versions
                // ensure the ZenRule is enabled
                val zenRule = notificationManager.getAutomaticZenRule(ruleId)
                if (zenRule != null && !zenRule.isEnabled) {
                    zenRule.isEnabled = true
                    val result = notificationManager.updateAutomaticZenRule(ruleId, zenRule)
                    Log.i("SoundProfileTileService", "Activated Automatic Zen Rule with status: ${result}.")
                }
                // directly set the interruption filter as fallback
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } else {
            // fallback to set the default interruption filter if rule does not exist
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    private fun deactivateAutomaticZenRule(ruleId: String) {
        /*
        It appears that disabling the interruption filter does not reset RingerMode to Normal, which is why it is set to Normal in any case.
         */
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (ruleId.isNotEmpty()) {
            // requires Android 10 / API 29 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val conditionId = ZenRuleUtils.SILENT_CONDITION_DND_AND_MODE_URI.toUri()
                val condition = Condition(conditionId, "Inactive", Condition.STATE_FALSE)
                notificationManager.setAutomaticZenRuleState(ruleId, condition)
            } else {
                // fallback for older Android versions
                // ensure the ZenRule is disabled
                val zenRule = notificationManager.getAutomaticZenRule(ruleId)
                if (zenRule != null && zenRule.isEnabled) {
                    zenRule.isEnabled = false
                    val result = notificationManager.updateAutomaticZenRule(ruleId, zenRule)
                    Log.i("SoundProfileTileService", "Deactivated Automatic Zen Rule with status: ${result}.")
                }
                // directly set the interruption filter as fallback
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } else {
            // fallback to set the default interruption filter if rule does not exist
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

}
