package com.curryplayer.quicksettingssoundprofile.factory

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import android.service.notification.ZenPolicy
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.curryplayer.quicksettingssoundprofile.MainActivity
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileConditionProviderService

object ZenRuleFactory {

    const val SILENT_CONDITION_DND_AND_MODE_URI =
        "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active"

    /**
     * Generates a default [AutomaticZenRule] tailored to the device's Android version.
     *
     * @param applicationContext The context used to access resources.
     * @param zenRuleIconIndex The ordinal index of the [IconTheme] to use for the rule's icon (Android 15+).
     * @return A configured [AutomaticZenRule].
     */
    fun createDefaultAutomaticZenRule(
        applicationContext: Context,
        zenRuleIconIndex: Int = IconTheme.VOLUME_DEFAULT.ordinal
    ): AutomaticZenRule {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            createDefaultAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(applicationContext, zenRuleIconIndex)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createDefaultAutomaticZenRuleForAndroidQAndAbove(applicationContext)
        } else {
            createDefaultAutomaticZenRuleForAndroidNAndAbove(applicationContext)
        }
    }

    // set baseline for Android 7 / API 24
    private fun createDefaultAutomaticZenRuleForAndroidNAndAbove(applicationContext: Context): AutomaticZenRule {
        // constructor is deprecated in API 35 but works for API 24
        return AutomaticZenRule(
            applicationContext.getString(R.string.zen_rule_name),
            ComponentName(applicationContext, SoundProfileConditionProviderService::class.java),
            SILENT_CONDITION_DND_AND_MODE_URI.toUri(),
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true
        )
    }

    // requires Android 10 / API 29
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createDefaultAutomaticZenRuleForAndroidQAndAbove(applicationContext: Context): AutomaticZenRule {
        val ruleName = applicationContext.getString(R.string.zen_rule_name)
        val configurationActivity = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()
        val zenPolicy: ZenPolicy = createDefaultZenPolicy()

        return AutomaticZenRule(
            ruleName,
            null, // superseded by configurationActivity
            configurationActivity,
            conditionId,
            zenPolicy,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true
        )
    }

    // requires Android 15 / API 35
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun createDefaultAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(
        applicationContext: Context,
        zenRuleIconIndex: Int
    ): AutomaticZenRule {
        val ruleName = applicationContext.getString(R.string.zen_rule_name)
        val configurationActivity = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()
        val zenPolicy: ZenPolicy = createDefaultZenPolicy()
        val zenDeviceEffects: ZenDeviceEffects = createDefaultZenDeviceEffects()
        val zenRuleIconResId = IconTheme.fromOrdinal(zenRuleIconIndex).silentIcon

        return AutomaticZenRule.Builder(ruleName, conditionId)
            .setOwner(null) // superseded by configurationActivity
            .setConfigurationActivity(configurationActivity)
            .setZenPolicy(zenPolicy)
            .setDeviceEffects(zenDeviceEffects)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setEnabled(true)
            .setIconResId(zenRuleIconResId)
            .setTriggerDescription(applicationContext.getString(R.string.zen_rule_trigger_description))
            .setManualInvocationAllowed(false)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .build()
    }

    /**
     * Builds a default [ZenPolicy] for the "Silent" profile.
     *
     * @return A [ZenPolicy] configured to disallow most interruptions while allowing alarms and media.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun createDefaultZenPolicy(): ZenPolicy {
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
            zenPolicyBuilder.allowPriorityChannels(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            zenPolicyBuilder.allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
        }

        return zenPolicyBuilder.build()
    }

    /**
     * Builds a default [ZenDeviceEffects] for the "Silent" profile.
     *
     * @return A [ZenDeviceEffects] instance with all effects (like grayscale or dimming) disabled.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun createDefaultZenDeviceEffects(): ZenDeviceEffects {
        return ZenDeviceEffects.Builder()
            .setShouldDimWallpaper(false)
            .setShouldUseNightMode(false)
            .setShouldDisplayGrayscale(false)
            .setShouldSuppressAmbientDisplay(false)
            .build()
    }

    /**
     * Creates a [Condition] with appropriate parameters based on the Android version.
     *
     * Starting with Android 15 (API level 35), the `source` parameter within a [Condition]
     * must be explicitly set to [Condition.SOURCE_USER_ACTION] for this condition to work properly.
     *
     * @param conditionId The unique URI of the condition.
     * @param summary A brief description of the condition's state.
     * @param state The state of the condition (e.g., [Condition.STATE_TRUE]).
     * @return A configured [Condition] object.
     */
    fun createCondition(conditionId: Uri, summary: String, state: Int): Condition {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Condition(
                conditionId,
                summary,
                state,
                Condition.SOURCE_USER_ACTION
            )
        } else {
            Condition(
                conditionId,
                summary,
                state
            )
        }
    }
}
