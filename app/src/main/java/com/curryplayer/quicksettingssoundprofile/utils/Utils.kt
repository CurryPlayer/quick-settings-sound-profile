package com.curryplayer.quicksettingssoundprofile.utils

import android.app.NotificationManager
import android.content.Context
import android.util.Log

object Utils {

    /**
     * Checks if the ACCESS_NOTIFICATION_POLICY (Do Not Disturb access) permission is granted.
     *
     * @param applicationContext The application context.
     * @return True if the permission is granted, false otherwise.
     */
    fun isDoNotDisturbPermissionGranted(applicationContext: Context): Boolean {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val granted = notificationManager.isNotificationPolicyAccessGranted
        if (!granted) {
            Log.w("PermissionCheck", "ACCESS_NOTIFICATION_POLICY (Do Not Disturb access) is NOT granted.")
        } else {
            Log.i("PermissionCheck", "ACCESS_NOTIFICATION_POLICY (Do Not Disturb access) is granted.")
        }
        return granted
    }

}