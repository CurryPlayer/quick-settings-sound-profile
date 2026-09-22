package com.curryplayer.quicksettingssoundprofile.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build

class RingerModeReceiver(
    private val context: Context,
    private val onRingerModeChanged: () -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if ((action == AudioManager.RINGER_MODE_CHANGED_ACTION) ||
            (action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)) {
            onRingerModeChanged()
        }
    }

    fun register() {
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION).apply {
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(this)
        } catch (_: IllegalArgumentException) {
            // Receiver previously not registered or already unregistered
        }
    }
}
