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
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileConditionProviderService

object ZenRuleUtils {

    const val SILENT_CONDITION_DND_AND_MODE_URI = "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active"
    // TODO: make name dynamic for multiple languages
    const val RULE_NAME = "Silence Profile Settings"


    fun isAutomaticZenRuleRegistered(applicationContext: Context, ruleId: String): Boolean {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.getAutomaticZenRule(ruleId) != null
    }

    fun generateDefaultAutomaticZenRule(applicationContext: Context): AutomaticZenRule {
        val zenRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            generateDefaultAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(applicationContext)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generateDefaultAutomaticZenRuleForAndroidQAndAbove(applicationContext)
        } else {
            generateDefaultAutomaticZenRuleForAndroidNAndAbove(applicationContext)
        }
        return zenRule
    }

    // set baseline for Android 7 / API 24
    private fun generateDefaultAutomaticZenRuleForAndroidNAndAbove(applicationContext: Context): AutomaticZenRule {
        // constructor is deprecated in API 35 but works for API 24
        val zenRule = AutomaticZenRule(
            RULE_NAME,
            ComponentName(applicationContext, SoundProfileConditionProviderService::class.java),
            SILENT_CONDITION_DND_AND_MODE_URI.toUri(),
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true
        )
        return zenRule
    }

    // requires Android 10 / API 29
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generateDefaultAutomaticZenRuleForAndroidQAndAbove(applicationContext: Context): AutomaticZenRule {

        val ruleName = RULE_NAME
        val owner = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = buildDefaultZenPolicy()

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

    // requires Android 15 / API 35
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun generateDefaultAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(applicationContext: Context): AutomaticZenRule {

        val ruleName = RULE_NAME
        val owner = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = buildDefaultZenPolicy()

        val zenRule = AutomaticZenRule.Builder(ruleName, conditionId)
            .setConfigurationActivity(owner)
            .setOwner(owner)
            .setZenPolicy(zenPolicy)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setEnabled(true)
            .setIconResId(R.drawable.ic_round_volume_off_24)
            .setTriggerDescription("When 'Silent' mode is activated via the Quick Settings Tile")   // TODO: localize string
            .setManualInvocationAllowed(false)
            .build()

        return zenRule
    }

    // TODO: make zenPolicy adjustable
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildDefaultZenPolicy(): ZenPolicy {
        val zenPolicyBuilder: ZenPolicy.Builder = ZenPolicy.Builder()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            zenPolicyBuilder
                .allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
                .allowPriorityChannels(false)
        }

        return zenPolicyBuilder.build()
    }

}