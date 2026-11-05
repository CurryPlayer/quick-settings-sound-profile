package com.curryplayer.quicksettingssoundprofile.scheduler

import com.curryplayer.quicksettingssoundprofile.models.AlarmItem

interface AlarmScheduler {
    suspend fun schedule(item: AlarmItem)
    suspend fun cancel()
}