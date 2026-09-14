package com.curryplayer.quicksettingssoundprofile.utils

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
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
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileConditionProviderService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds

object ZenRuleUtils {

    const val SILENT_CONDITION_DND_AND_MODE_URI = "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active"
    private val SYNCHRONIZATION_DURATION_DELAY_MS = 50.milliseconds
    private const val MAX_SYNCHRONIZATION_RETIRES = 10

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
                val savedIconThemeIndex = dataStoreManager.iconTheme.first()
                val newRule = generateDefaultAutomaticZenRule(applicationContext, savedIconThemeIndex)
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

    /**
     * Generates a default [AutomaticZenRule] tailored to the device's Android version.
     *
     * @param applicationContext The context used to access resources.
     * @param zenRuleIconIndex The ordinal index of the [IconTheme] to use for the rule's icon (Android 15+).
     * @return A configured [AutomaticZenRule].
     */
    fun generateDefaultAutomaticZenRule(applicationContext: Context, zenRuleIconIndex: Int = IconTheme.VOLUME_DEFAULT.ordinal): AutomaticZenRule {
        val zenRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            generateDefaultAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(applicationContext, zenRuleIconIndex)
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
    private fun generateDefaultAutomaticZenRuleForAndroidVanillaIceCreamAndAbove(
        applicationContext: Context,
        zenRuleIconIndex: Int
    ): AutomaticZenRule {

        val ruleName = applicationContext.getString(R.string.zen_rule_name)
        val configurationActivity = ComponentName(applicationContext, MainActivity::class.java)
        val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()

        val zenPolicy: ZenPolicy = buildDefaultZenPolicy()
        val zenDeviceEffects: ZenDeviceEffects = buildDefaultZenDeviceEffects()

        val zenRuleIconResId = IconTheme.fromOrdinal(zenRuleIconIndex).silentIcon

        val zenRule = AutomaticZenRule.Builder(ruleName, conditionId)
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

        return zenRule
    }

    /**
     * Updates the icon of an existing [AutomaticZenRule].
     *
     * This feature requires Android 15 (API level 35) or higher.
     *
     * @param context The context used to access the [NotificationManager].
     * @param ruleId The unique identifier of the rule to update.
     * @param iconResId The resource ID of the new icon.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun updateZenRuleIcon(context: Context, ruleId: String, iconResId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existingRule = notificationManager.getAutomaticZenRule(ruleId)
        if (existingRule != null) {
            val updatedRule = AutomaticZenRule.Builder(existingRule)
                .setIconResId(iconResId)
                .build()
            notificationManager.updateAutomaticZenRule(ruleId, updatedRule)
        }
    }

    /**
     * Builds a default [ZenPolicy] for the "Silent" profile.
     *
     * @return A [ZenPolicy] configured to disallow most interruptions while allowing alarms and media.
     */
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

    /**
     * Builds a default [ZenDeviceEffects] for the "Silent" profile.
     *
     * @return A [ZenDeviceEffects] instance with all effects (like grayscale or dimming) disabled.
     */
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
     * Creates a [Condition] with appropriate parameters based on the Android version.
     *
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
     *
     * @param conditionId The unique URI of the condition.
     * @param summary A brief description of the condition's state.
     * @param state The state of the condition (e.g., [Condition.STATE_TRUE]).
     * @return A configured [Condition] object.
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

    /**
     * Safely applies the [AutomaticZenRule] state and ensures the [AudioManager.ringerMode] is synchronized.
     *
     * Due to the asynchronous nature of Android's system services, directly chaining Do Not Disturb (DND)
     * changes with ringer mode changes can cause race conditions (e.g., the `AudioService` ignoring commands
     * or getting stuck in `SILENT` mode).
     *
     * This method resolves these issues by applying the [AutomaticZenRule] first and synchronizes the [AudioManager.ringerMode] afterward.
     *
     * @param context The context used to access the [NotificationManager] and [AudioManager].
     * @param ruleId The unique identifier of the rule to apply.
     * @param activate True if the rule should be activated, false otherwise.
     * @param targetRingerMode The desired ringer mode after activating / deactivating the rule.
     */
    // the correct way to handle this would probably be to implement it using intents and a broadcast receiver.
    suspend fun applyZenRuleAndRingerMode(
        context: Context,
        ruleId: String,
        activate: Boolean,
        targetRingerMode: Int
    ) {

        val audioManager = context.getSystemService(AudioManager::class.java)

        // set ringerMode to 'NORMAL' before activating zenRule to allow notification sounds for excluded content (contacts, apps, etc.)
        // should this be a toggleable option??
        val needToAdjustRingerMode = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
        if (activate && needToAdjustRingerMode) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }

        setAutomaticZenRuleState(context, ruleId, activate)

        if (activate) {
            // waiting for ringerMode 'SILENT' to get synchronized
            var retries = 0
            while (audioManager.ringerMode != targetRingerMode && retries < MAX_SYNCHRONIZATION_RETIRES) {
                delay(SYNCHRONIZATION_DURATION_DELAY_MS)
                retries++
            }
            // fallback: set ringer mode manually (this should always be 'SILENT'!)
            if (audioManager.ringerMode != targetRingerMode) {
                audioManager.ringerMode = targetRingerMode
            }
        } else {
            // applying ringerMode 'NORMAL' or 'VIBRATE' until it is synchronized
            var retries = 0
            while (audioManager.ringerMode != targetRingerMode && retries < MAX_SYNCHRONIZATION_RETIRES) {
                audioManager.ringerMode = targetRingerMode
                delay(SYNCHRONIZATION_DURATION_DELAY_MS)
                retries++
            }
        }
    }

    /**
     * Sets the state of an AutomaticZenRule (or fallback interruption filter).
     *
     * @param context The context used to access the [NotificationManager].
     * @param ruleId The unique identifier of the rule to update.
     * @param activate True if the rule should be activated, false otherwise.
     */
    fun setAutomaticZenRuleState(context: Context, ruleId: String, activate: Boolean) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val interruptionFilter = if (activate) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL

        if (ruleId.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val conditionId = SILENT_CONDITION_DND_AND_MODE_URI.toUri()
                val summary = if (activate) "Active" else "Inactive"
                val state = if (activate) Condition.STATE_TRUE else Condition.STATE_FALSE
                val condition = buildCondition(conditionId, summary, state)
                notificationManager.setAutomaticZenRuleState(ruleId, condition)
            } else {
                val zenRule = notificationManager.getAutomaticZenRule(ruleId)
                if (zenRule != null && zenRule.isEnabled != activate) {
                    zenRule.isEnabled = activate
                    notificationManager.updateAutomaticZenRule(ruleId, zenRule)
                }
                notificationManager.setInterruptionFilter(interruptionFilter)
            }
        } else {
            notificationManager.setInterruptionFilter(interruptionFilter)
        }
    }

}