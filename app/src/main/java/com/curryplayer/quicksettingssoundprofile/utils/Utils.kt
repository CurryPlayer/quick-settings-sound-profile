package com.curryplayer.quicksettingssoundprofile.utils

import android.app.NotificationManager
import android.content.Context

object Utils {


    /**
     * Checks if the ACCESS_NOTIFICATION_POLICY (Do Not Disturb access) permission is granted.
     *
     * @param applicationContext The application context.
     * @return True if the permission is granted, false otherwise.
     */
    fun isDoNotDisturbPermissionGranted(applicationContext: Context): Boolean {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

}