package com.curryplayer.quicksettingssoundprofile.models

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Describes how the device handles the modes (automatic zen rules) of the silent profile.
 */
enum class ZenModesSupport {
    /** Android < 15: the modes cannot be customized per rule. */
    NotSupported,

    /** Android >= 15 and the device lets the user manage modes. */
    UserManaged,

    /** Android >= 15, but the device only uses schedules which cannot be modified directly. */
    ScheduleOnly;

    companion object {
        /**
         * Determines which [ZenModesSupport] the current device offers.
         *
         * Not every device running Android 15 has user managed modes. These devices use
         * schedules instead, which cannot be modified directly.
         */
        fun resolveDeviceZenModesSupport(ctx: Context): ZenModesSupport {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                return NotSupported
            }
            val notificationManager = ctx.getSystemService(NotificationManager::class.java)
            return if (notificationManager.areAutomaticZenRulesUserManaged()) UserManaged else ScheduleOnly
        }
    }
}