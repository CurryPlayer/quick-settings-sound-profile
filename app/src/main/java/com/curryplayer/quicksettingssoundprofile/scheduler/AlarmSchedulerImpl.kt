package com.curryplayer.quicksettingssoundprofile.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.models.AlarmItem
import com.curryplayer.quicksettingssoundprofile.receivers.TimerExpiredReceiver

class AlarmSchedulerImpl(
    private val context: Context,
    private val dataStoreManager: DataStoreManager = DataStoreManager(context)
) : AlarmScheduler {

    companion object {
        private const val TIMER_REQUEST_CODE: Int = 1001
    }

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)

    override suspend fun schedule(item: AlarmItem) {
        val durationMinutes = item.durationMinutes
        val previousMode = audioManager.ringerMode

        val durationMillis = durationMinutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + durationMillis

        // Save timer and last duration in DataStore
        dataStoreManager.saveMuteDurationMinutes(durationMinutes)
        dataStoreManager.saveTimer(endTime, previousMode)

        // Schedule AlarmManager
        val intent = Intent(context, TimerExpiredReceiver::class.java)
        // TODO: check requestCode
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        endTime,
                        pendingIntent
                    )
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC, endTime, pendingIntent)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC, endTime, pendingIntent)
            }
        }
    }

    override suspend fun cancel() {
        val intent = Intent(context, TimerExpiredReceiver::class.java)
        // TODO: check requestCode
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        dataStoreManager.clearTimer()
    }
}