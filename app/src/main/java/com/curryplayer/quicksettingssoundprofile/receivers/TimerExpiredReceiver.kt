package com.curryplayer.quicksettingssoundprofile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerExpiredReceiver : BroadcastReceiver() {

    // TODO: remove logs, check logic
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        Log.i("TimerExpiredReceiver", pendingResult.toString())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i("TimerExpiredReceiver", "Timer expired")
                val dataStoreManager = DataStoreManager(context)
                val previousMode = dataStoreManager.previousRingerMode.first()
                Log.i("TimerExpiredReceiver", "previousMode = $previousMode")

                if (previousMode != -1) {
                    val ruleId = dataStoreManager.zenRuleId.first()
                    val activateZenRule = (previousMode == AudioManager.RINGER_MODE_SILENT)
                    ZenRuleUtils.applyZenRuleAndRingerMode(context, ruleId, activateZenRule, previousMode)

                    dataStoreManager.clearTimer()
                }
            } catch (_: Exception) {
                // Ignore
            } finally {
                pendingResult.finish()
            }
        }
    }
}
