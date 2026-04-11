package com.curryplayer.quicksettingssoundprofile.utils

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.ZenPolicy
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.curryplayer.quicksettingssoundprofile.MainActivity

object Utils {

    private const val SILENT_CONDITION_DND_AND_MODE_URI = "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active"

    // TODO: make name dynamic for multiple languages
    private const val RULE_NAME = "Silence Profile Settings"

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

    // set baseline for Android 7 / API 24
    fun generateAutomaticZenRuleForAndroidNAndAbove(applicationContext: Context): AutomaticZenRule {
        // constructor is deprecated in API 35 but works for API 24
        val zenRule = AutomaticZenRule(
            RULE_NAME,
            ComponentName(applicationContext, MainActivity::class.java),
            SILENT_CONDITION_DND_AND_MODE_URI.toUri(),
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true
        )
        return zenRule
    }

    // TODO: make zenRule adjustable
    // requires Android 10 / API 29
    @RequiresApi(Build.VERSION_CODES.Q)
    fun generateAutomaticZenRuleForAndroidQAndAbove(applicationContext: Context): AutomaticZenRule {

        val ruleName = RULE_NAME
        val owner = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = ZenPolicy.Builder()
            .disallowAllSounds()
            .allowMedia(true)
            .allowAlarms(true)
            .allowSystem(false)
            .allowReminders(false)
            .allowEvents(false)
            .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowRepeatCallers(false)
            .showFullScreenIntent(false)
            .showLights(false)
            .showPeeking(false)
            .showStatusBarIcons(true)
            .showBadges(true)
            .showInAmbientDisplay(false)
            .showInNotificationList(true)
            .build()

        val zenRule = AutomaticZenRule(
            ruleName,
            owner,
            owner,
            conditionId,
            zenPolicy,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true
        )

        return zenRule
    }

    // TODO: make zenRule adjustable
    // requires Android 15 / API 35
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun generateAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(applicationContext: Context): AutomaticZenRule {

        val ruleName = RULE_NAME
        val owner = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = ZenPolicy.Builder()
            .disallowAllSounds()
            .allowMedia(true)
            .allowAlarms(true)
            .allowSystem(false)
            .allowReminders(false)
            .allowEvents(false)
            .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowRepeatCallers(false)
            .showFullScreenIntent(false)
            .showLights(false)
            .showPeeking(false)
            .showStatusBarIcons(true)
            .showBadges(true)
            .showInAmbientDisplay(false)
            .showInNotificationList(true)
            .allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
            .allowPriorityChannels(false)
            .build()

        val zenRule = AutomaticZenRule.Builder(ruleName, conditionId)
            .setConfigurationActivity(owner)
            .setOwner(owner)
            .setZenPolicy(zenPolicy)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setEnabled(true)
            .build()

        return zenRule
    }

}