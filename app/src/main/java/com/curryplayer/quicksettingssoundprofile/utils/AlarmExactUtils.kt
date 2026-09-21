package com.curryplayer.quicksettingssoundprofile.utils

import android.app.AlarmManager
import android.content.Context
import android.os.Build

object AlarmExactUtils {

    /**
     * Checks if the SCHEDULE_EXACT_ALARM permission is granted.
     *
     * @param ctx The application context.
     * @return True if the permission is granted, false otherwise.
     */
    fun isScheduleExactAlarmsPermissionGranted(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
    }

}