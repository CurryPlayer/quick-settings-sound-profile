package com.curryplayer.quicksettingssoundprofile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.curryplayer.quicksettingssoundprofile.manager.SoundProfileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val soundProfileManager = SoundProfileManager(context)
                soundProfileManager.restorePreviousMode()
            } catch (_: Exception) {
                // Ignore
            } finally {
                pendingResult.finish()
            }
        }
    }
}
