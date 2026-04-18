package com.curryplayer.quicksettingssoundprofile.utils

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.ZenDeviceEffects
import android.service.notification.ZenPolicy
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.curryplayer.quicksettingssoundprofile.MainActivity
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileConditionProviderService
import kotlinx.coroutines.flow.first

object ZenRuleUtils {

    const val SILENT_CONDITION_DND_AND_MODE_URI = "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active"
    // TODO: make name dynamic for multiple languages
    const val RULE_NAME = "Silence Profile Settings"


    suspend fun syncAutomaticZenRule(
        applicationContext: Context,
        dataStoreManager: DataStoreManager
    ): String {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var savedRuleId = dataStoreManager.zenRuleId.first()

        // check if there is already an existing rule with the same name
        if (savedRuleId.isEmpty()) {
            val allRules = notificationManager.automaticZenRules
            val existingRuleEntry = allRules.entries.find { it.value.name == RULE_NAME }
            if (existingRuleEntry != null) {
                savedRuleId = existingRuleEntry.key
                dataStoreManager.setZenRuleId(savedRuleId)
                Log.i("MainActivity", "Found existing rule: $savedRuleId")
            }
        }

        // check if valid rule exists
        val existingRule = if (savedRuleId.isNotEmpty()) notificationManager.getAutomaticZenRule(savedRuleId) else null

        if (existingRule == null) {
            val newRule = generateDefaultAutomaticZenRule(applicationContext)
            val newId = notificationManager.addAutomaticZenRule(newRule)
            if (newId != null) {
                dataStoreManager.setZenRuleId(newId)
                Log.i("MainActivity", "New ZenRule created: $newId")
                return newId
            }
            Log.e("MainActivity", "Could not create new ZenRule")
            return ""
        } else {
            // TODO: maybe update the rule here if changes were made to it
            // val updatedRule = existingRule
            /*
            TODO:
            Before Build.VERSION_CODES.VANILLA_ICE_CREAM, updating a rule that is not backed up
            by a android.service.notification.ConditionProviderService will deactivate it if it
            was previously active. Starting with Build.VERSION_CODES.VANILLA_ICE_CREAM, this will
            only happen if the rule's definition is actually changing.
             */
            // val success = notificationManager.updateAutomaticZenRule(savedRuleId, updatedRule)
            Log.i("MainActivity", "ZenRule already exists: $savedRuleId")
            return savedRuleId
        }
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
        val zenDeviceEffects: ZenDeviceEffects = buildDefaultZenDeviceEffects()

        val zenRule = AutomaticZenRule.Builder(ruleName, conditionId)
            .setConfigurationActivity(owner)
            .setOwner(owner)
            .setZenPolicy(zenPolicy)
            .setDeviceEffects(zenDeviceEffects)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setEnabled(true)
            .setIconResId(R.drawable.ic_round_volume_off_24)
            .setTriggerDescription("When 'Silent' mode is activated via the Quick Settings Tile")   // TODO: localize string
            .setManualInvocationAllowed(false)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .build()

        return zenRule
    }

    // TODO: make zenPolicy adjustable
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildDefaultZenPolicy(): ZenPolicy {
        val zenPolicyBuilder: ZenPolicy.Builder = ZenPolicy.Builder()
            .disallowAllSounds()
            .showAllVisualEffects()
            .allowMedia(true)
            .allowAlarms(true)
            .allowSystem(false)
            .allowReminders(false)
            .allowEvents(false)
            .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowRepeatCallers(false)
            .showFullScreenIntent(true)
            .showLights(true)
            .showPeeking(true)
            .showStatusBarIcons(true)
            .showBadges(true)
            .showInAmbientDisplay(true)
            .showInNotificationList(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            zenPolicyBuilder
                .allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
                .allowPriorityChannels(false)
        }

        return zenPolicyBuilder.build()
    }

    // TODO: maybe use in future or delete
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun modifyDefaultZenPolicy(oldZenPolicy: ZenPolicy): ZenPolicy {
//        val newZenPolicyBuilder: ZenPolicy.Builder = ZenPolicy.Builder()
//            .disallowAllSounds()
//            .showAllVisualEffects()
//            .allowMedia(oldZenPolicy.priorityCategoryMedia == ZenPolicy.STATE_ALLOW)
//            .allowAlarms(oldZenPolicy.priorityCategoryAlarms == ZenPolicy.STATE_ALLOW)
//            .allowSystem(oldZenPolicy.priorityCategorySystem == ZenPolicy.STATE_ALLOW)
//            .allowReminders(oldZenPolicy.priorityCategoryReminders == ZenPolicy.STATE_ALLOW)
//            .allowEvents(oldZenPolicy.priorityCategoryEvents == ZenPolicy.STATE_ALLOW)
//            .allowCalls(oldZenPolicy.priorityCallSenders)
//            .allowMessages(oldZenPolicy.priorityMessageSenders)
//            .allowRepeatCallers(oldZenPolicy.priorityCategoryRepeatCallers == ZenPolicy.STATE_ALLOW)
//            .showFullScreenIntent(oldZenPolicy.visualEffectFullScreenIntent == ZenPolicy.STATE_ALLOW)
//            .showLights(oldZenPolicy.visualEffectLights == ZenPolicy.STATE_ALLOW)
//            .showPeeking(oldZenPolicy.visualEffectPeek == ZenPolicy.STATE_ALLOW)
//            .showStatusBarIcons(oldZenPolicy.visualEffectStatusBar == ZenPolicy.STATE_ALLOW)
//            .showBadges(oldZenPolicy.visualEffectBadge == ZenPolicy.STATE_ALLOW)
//            .showInAmbientDisplay(oldZenPolicy.visualEffectAmbient == ZenPolicy.STATE_ALLOW)
//            .showInNotificationList(oldZenPolicy.visualEffectNotificationList == ZenPolicy.STATE_ALLOW)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
//            newZenPolicyBuilder
//                .allowConversations(oldZenPolicy.priorityConversationSenders)
//                .allowPriorityChannels(oldZenPolicy.priorityChannelsAllowed == ZenPolicy.STATE_ALLOW)
//        }
//
//        return newZenPolicyBuilder.build()
//    }

    // TODO: make zenDeviceEffects adjustable
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun buildDefaultZenDeviceEffects(): ZenDeviceEffects {
        val zenDeviceEffects: ZenDeviceEffects = ZenDeviceEffects.Builder()
            .setShouldDimWallpaper(false)
            .setShouldUseNightMode(false)
            .setShouldDisplayGrayscale(false)
            .setShouldSuppressAmbientDisplay(false)
            .build()

        return zenDeviceEffects
    }

//    // TODO: maybe use in future or delete
//    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
//    private fun modifyDefaultZenDeviceEffects(oldZenDeviceEffects: ZenDeviceEffects): ZenDeviceEffects {
//        val newZenDeviceEffects: ZenDeviceEffects = ZenDeviceEffects.Builder()
//            .setShouldDimWallpaper(oldZenDeviceEffects.shouldDimWallpaper())
//            .setShouldUseNightMode(oldZenDeviceEffects.shouldUseNightMode())
//            .setShouldDisplayGrayscale(oldZenDeviceEffects.shouldDisplayGrayscale())
//            .setShouldSuppressAmbientDisplay(oldZenDeviceEffects.shouldSuppressAmbientDisplay())
//            .build()
//
//        return newZenDeviceEffects
//    }

}