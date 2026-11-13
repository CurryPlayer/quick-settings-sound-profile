package com.curryplayer.quicksettingssoundprofile.manager

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.Condition
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.factory.ZenRuleFactory
import kotlinx.coroutines.flow.first

/**
 * Manages the lifecycle and state of the app's [AutomaticZenRule] via [NotificationManager].
 *
 * @param context The context used to access system services and resources.
 * @property dataStoreManager Manages persisted ZenRule ID and icon theme settings.
 * @property notificationManager Android system service for ZenRules and notification policies.
 * @property zenRuleFactory Factory responsible for constructing version-specific ZenRule components.
 */
class ZenRuleManager(
    private val context: Context,
    val dataStoreManager: DataStoreManager = DataStoreManager(context),
    private val notificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java),
    private val zenRuleFactory: ZenRuleFactory = ZenRuleFactory,
) {
    private val appContext: Context = context.applicationContext ?: context

    /**
     * Ensures a valid [AutomaticZenRule] exists by retrieving a saved ID or searching
     * for an existing rule by name. If no rule is found, it automatically creates and persists
     * a new one to maintain the app's silent profile functionality.
     *
     * @return The ID of the [AutomaticZenRule], or an empty string if the rule could not
     * be found or created.
     */
    suspend fun syncAutomaticZenRule(): String {
        var savedRuleId = dataStoreManager.zenRuleId.first()

        // Check if there is already an existing rule with the same name
        if (savedRuleId.isEmpty()) {
            val targetRuleName = appContext.getString(R.string.zen_rule_name)
            val existingRuleId = notificationManager.automaticZenRules?.entries
                ?.firstOrNull { it.value?.name == targetRuleName }
                ?.key

            if (!existingRuleId.isNullOrEmpty()) {
                savedRuleId = existingRuleId
                dataStoreManager.setZenRuleId(savedRuleId)
            }
        }

        // Check if valid rule exists
        val existingRule = if (savedRuleId.isNotEmpty()) notificationManager.getAutomaticZenRule(savedRuleId) else null

        if (existingRule == null) {
            try {
                val savedIconThemeIndex = dataStoreManager.iconTheme.first()
                val newRule = zenRuleFactory.createDefaultAutomaticZenRule(appContext, savedIconThemeIndex)
                val newId = notificationManager.addAutomaticZenRule(newRule)
                if (newId != null) {
                    dataStoreManager.setZenRuleId(newId)
                    return newId
                }
            } catch (_: Exception) {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.toast_create_rule_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
            return ""
        } else {
            return savedRuleId
        }
    }

    /**
     * Retrieves an [AutomaticZenRule] by its ID.
     */
    fun getAutomaticZenRule(ruleId: String): AutomaticZenRule? {
        return if (ruleId.isNotEmpty()) notificationManager.getAutomaticZenRule(ruleId) else null
    }

    /**
     * Updates the icon of an existing [AutomaticZenRule].
     *
     * This feature requires Android 15 (API level 35) or higher.
     *
     * @param ruleId The unique identifier of the rule to update.
     * @param iconResId The resource ID of the new icon.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun updateZenRuleIcon(ruleId: String, iconResId: Int) {
        val existingRule = getAutomaticZenRule(ruleId)
        if (existingRule != null) {
            val updatedRule = AutomaticZenRule.Builder(existingRule)
                .setIconResId(iconResId)
                .build()
            notificationManager.updateAutomaticZenRule(ruleId, updatedRule)
        }
    }

    /**
     * Sets the state of an [AutomaticZenRule] (or fallback interruption filter).
     *
     * @param ruleId The unique identifier of the rule to update.
     * @param activate True if the rule should be activated, false otherwise.
     */
    fun setAutomaticZenRuleState(ruleId: String, activate: Boolean) {
        val interruptionFilter = if (activate) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }

        if (ruleId.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val conditionId = ZenRuleFactory.SILENT_CONDITION_DND_AND_MODE_URI.toUri()
                val summary = if (activate) "Active" else "Inactive"
                val state = if (activate) Condition.STATE_TRUE else Condition.STATE_FALSE
                val condition = zenRuleFactory.createCondition(conditionId, summary, state)
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

    /**
     * Checks if the ZenRule is confirmed deactivated on Android 15+.
     */
    fun isRuleDeactivated(ruleId: String): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && ruleId.isNotEmpty() && try {
            notificationManager.getAutomaticZenRuleState(ruleId) == Condition.STATE_FALSE
        } catch (_: Exception) {
            false
        }
    }
}
