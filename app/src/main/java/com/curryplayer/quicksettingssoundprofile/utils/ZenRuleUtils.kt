package com.curryplayer.quicksettingssoundprofile.utils

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import android.service.notification.ZenPolicy
//import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.curryplayer.quicksettingssoundprofile.MainActivity
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileConditionProviderService
import kotlinx.coroutines.flow.first

object ZenRuleUtils {

    const val SILENT_CONDITION_DND_AND_MODE_URI = "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active"

    /**
     * This method ensures a valid AutomaticZenRule exists by retrieving a saved ID or searching
     * for an existing rule by name. If no rule is found, it automatically creates and persists
     * a new one to maintain the app's silent profile functionality.
     *
     * @param applicationContext The context used to access system services and resources.
     * @param dataStoreManager The manager used to retrieve and persist the unique ZenRule ID.
     * @return The ID of the [AutomaticZenRule], or an empty string if the rule could not
     * be found or created.
     */
    suspend fun syncAutomaticZenRule(
        applicationContext: Context,
        dataStoreManager: DataStoreManager
    ): String {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var savedRuleId = dataStoreManager.zenRuleId.first()

        // check if there is already an existing rule with the same name
        if (savedRuleId.isEmpty()) {
            val allRules = notificationManager.automaticZenRules
            val existingRuleEntry = allRules.entries.find { it.value.name == applicationContext.getString(R.string.zen_rule_name) }
            if (existingRuleEntry != null) {
                savedRuleId = existingRuleEntry.key
                dataStoreManager.setZenRuleId(savedRuleId)
            }
        }

        // check if valid rule exists
        val existingRule = if (savedRuleId.isNotEmpty()) notificationManager.getAutomaticZenRule(savedRuleId) else null

        if (existingRule == null) {
            try {
                val newRule = generateDefaultAutomaticZenRule(applicationContext)
                val newId = notificationManager.addAutomaticZenRule(newRule)
                if (newId != null) {
                    dataStoreManager.setZenRuleId(newId)
                    //Log.i("ZenRuleUtils", "New ZenRule created: $newId")
                    return newId
                }
            } catch (_: Exception) {
                //Log.i("ZenRuleUtils", "Could not create ZenRule")
                Toast.makeText(
                    applicationContext,
                    applicationContext.getString(R.string.toast_create_rule_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
            return ""
        } else {
            //Log.i("ZenRuleUtils", "ZenRule already exists: $savedRuleId")
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
            applicationContext.getString(R.string.zen_rule_name),
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

        val ruleName = applicationContext.getString(R.string.zen_rule_name)
        val configurationActivity = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = buildDefaultZenPolicy()

        val zenRule = AutomaticZenRule(
            ruleName,
            null,   // superseded by configurationActivity
            configurationActivity,
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

        val ruleName = applicationContext.getString(R.string.zen_rule_name)
        val configurationActivity = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = buildDefaultZenPolicy()
        val zenDeviceEffects: ZenDeviceEffects = buildDefaultZenDeviceEffects()

        val zenRule = AutomaticZenRule.Builder(ruleName, conditionId)
            .setOwner(null) // superseded by configurationActivity
            .setConfigurationActivity(configurationActivity)
            .setZenPolicy(zenPolicy)
            .setDeviceEffects(zenDeviceEffects)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setEnabled(true)
            .setIconResId(R.drawable.ic_round_volume_off_24)
            .setTriggerDescription(applicationContext.getString(R.string.zen_rule_trigger_description))
            .setManualInvocationAllowed(false)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .build()

        return zenRule
    }

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
                .allowPriorityChannels(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            zenPolicyBuilder
                .allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
        }

        return zenPolicyBuilder.build()
    }

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

    /**
     * Starting with Android 15 (API level 35), the `source` parameter within a [Condition]
     * must be explicitly set to [Condition.SOURCE_USER_ACTION] for this condition to work properly.
     *
     * This is because if a user manually deactivates a custom Zen Mode (e.g., via the system UI mode
     * selector) -- and this app creates such a custom Zen Rule for the "Silent" mode -- the Android system blocks that
     * specific custom rule from being applied on the immediate next execution.
     * Without this parameter, the custom Zen Rule would only successfully
     * activate on the *second* attempt to enable the "Silent" mode.
     *
     * Providing [Condition.SOURCE_USER_ACTION] as the source resolves this exact issue on devices running Android 15 or higher.
     */
    fun buildCondition(conditionId: Uri, summary: String, state: Int): Condition {
        val condition: Condition =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
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
        return condition
    }

}