package com.curryplayer.quicksettingssoundprofile.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object PermissionUtils {

    /**
     * Checks if the ACCESS_NOTIFICATION_POLICY (Do Not Disturb access) permission is granted.
     *
     * @param context The application context.
     * @return True if the permission is granted, false otherwise.
     */
    fun isDoNotDisturbPermissionGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Checks if the SCHEDULE_EXACT_ALARM permission is granted.
     *
     * @param context The application context.
     * @return True if the permission is granted, false otherwise.
     */
    fun isScheduleExactAlarmsPermissionGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
}
