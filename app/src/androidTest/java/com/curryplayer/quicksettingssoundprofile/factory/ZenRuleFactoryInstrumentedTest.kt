package com.curryplayer.quicksettingssoundprofile.factory

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import android.service.notification.ZenPolicy
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.curryplayer.quicksettingssoundprofile.MainActivity
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileConditionProviderService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Characterization tests for [ZenRuleFactory].
 *
 * They pin every value of the created [AutomaticZenRule], [ZenPolicy] and [ZenDeviceEffects] so that
 * neither a code change nor a changed Android framework default can silently alter the rule.
 *
 * Note: [ZenRuleFactory.createDefaultAutomaticZenRule] picks its implementation by `Build.VERSION.SDK_INT`,
 * so only the branch matching the device the test runs on is exercised. Run the suite on API 24-28,
 * API 29-34 and API 35+ to cover all branches.
 *
 * IMPORTANT: Do not use classes that don't exist on API 24 ([ZenPolicy], [ZenDeviceEffects]) in method
 * signatures (parameters or return types) of this class. JUnit resolves them via reflection and the whole
 * class would fail with `initializationError` on older devices. Put such helpers in a separate class,
 * see [ZenPolicyAssertions] and [ZenDeviceEffectsAssertions].
 */
@RunWith(AndroidJUnit4::class)
class ZenRuleFactoryInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun silentConditionUri_isUnchanged() {
        // Intentionally a hard-coded literal: changing the constant would orphan existing rules on user devices.
        assertEquals(
            "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active",
            ZenRuleFactory.SILENT_CONDITION_DND_AND_MODE_URI,
        )
    }

    @Test
    fun createDefaultAutomaticZenRule_withDefaultIconIndex_hasExpectedValues() {
        val rule = ZenRuleFactory.createDefaultAutomaticZenRule(context)

        verifyAutomaticZenRule(rule, IconTheme.VOLUME_DEFAULT)
    }

    @Test
    fun createDefaultAutomaticZenRule_forEveryIconTheme_hasExpectedValues() {
        IconTheme.entries.forEach { iconTheme ->
            val rule = ZenRuleFactory.createDefaultAutomaticZenRule(context, iconTheme.ordinal)

            verifyAutomaticZenRule(rule, iconTheme)
        }
    }

    private fun verifyAutomaticZenRule(rule: AutomaticZenRule, expectedIconTheme: IconTheme) {
        // Values common to all API levels
        assertEquals(
            "Rule name must match resource string",
            context.getString(R.string.zen_rule_name),
            rule.name
        )
        assertEquals(
            "Condition URI must match SILENT_CONDITION_DND_AND_MODE_URI",
            ZenRuleFactory.SILENT_CONDITION_DND_AND_MODE_URI.toUri(),
            rule.conditionId,
        )
        assertEquals(
            "Interruption filter must be PRIORITY",
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            rule.interruptionFilter,
        )
        assertTrue("Rule must be enabled", rule.isEnabled)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            assertNull(
                "Owner must be null on API 35+",
                rule.owner
            )
            assertEquals(
                "Configuration activity must be MainActivity",
                ComponentName(context, MainActivity::class.java),
                rule.configurationActivity,
            )

            assertNotNull(
                "ZenPolicy must not be null on API 35+",
                rule.zenPolicy
            )
            ZenPolicyAssertions.verifyDefaultZenPolicy(rule.zenPolicy!!)

            assertNotNull(
                "ZenDeviceEffects must not be null on API 35+",
                rule.deviceEffects
            )
            ZenDeviceEffectsAssertions.verifyDefaultZenDeviceEffects(rule.deviceEffects!!)

            assertEquals(
                "Icon res ID must match selected IconTheme silentIcon",
                expectedIconTheme.silentIcon,
                rule.iconResId,
            )
            assertEquals(
                "Trigger description must match resource string",
                context.getString(R.string.zen_rule_trigger_description),
                rule.triggerDescription,
            )
            assertFalse(
                "Manual invocation allowed must be false",
                rule.isManualInvocationAllowed
            )
            assertEquals(
                "Type must be TYPE_OTHER",
                AutomaticZenRule.TYPE_OTHER,
                rule.type
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertNull(
                "Owner must be null on API 29-34",
                rule.owner
            )
            assertEquals(
                "Configuration activity must be MainActivity",
                ComponentName(context, MainActivity::class.java),
                rule.configurationActivity,
            )

            assertNotNull(
                "ZenPolicy must not be null on API 29-34",
                rule.zenPolicy
            )
            ZenPolicyAssertions.verifyDefaultZenPolicy(rule.zenPolicy!!)
        } else {
            assertEquals(
                "Owner must be SoundProfileConditionProviderService on API 24-28",
                ComponentName(context, SoundProfileConditionProviderService::class.java),
                rule.owner,
            )
            // No assertion for configurationActivity here: AutomaticZenRule.getConfigurationActivity()
            // only exists since API 29 and throws NoSuchMethodError on API 24-28.
        }
    }

    @Test
    fun createDefaultZenPolicy_hasExpectedValues() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ZenPolicyAssertions.verifyDefaultZenPolicy(ZenRuleFactory.createDefaultZenPolicy())
        }
    }

    @Test
    fun createDefaultZenDeviceEffects_hasExpectedValues() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ZenDeviceEffectsAssertions.verifyDefaultZenDeviceEffects(ZenRuleFactory.createDefaultZenDeviceEffects())
        }
    }

    @Test
    fun createCondition_withStateTrue_hasExpectedValues() {
        verifyCondition(Condition.STATE_TRUE)
    }

    @Test
    fun createCondition_withStateFalse_hasExpectedValues() {
        verifyCondition(Condition.STATE_FALSE)
    }

    private fun verifyCondition(state: Int) {
        val conditionId: Uri = "condition://test/uri".toUri()
        val summary = "Test Summary"

        val condition = ZenRuleFactory.createCondition(conditionId, summary, state)

        assertEquals(
            "Condition ID must match",
            conditionId, condition.id
        )
        assertEquals(
            "Condition summary must match",
            summary, condition.summary
        )
        assertEquals(
            "Condition state must match",
            state, condition.state
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            assertEquals(
                "Condition source must be SOURCE_USER_ACTION on API 35+",
                Condition.SOURCE_USER_ACTION,
                condition.source,
            )
        }
    }
}
