package com.curryplayer.quicksettingssoundprofile.factory

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [ZenRuleFactory] running on Robolectric JVM environment.
 *
 * They pin every value of the created [AutomaticZenRule], `ZenPolicy`, `ZenDeviceEffects`, and
 * [Condition] across all supported Android API branches (`N`, `Q`, `R`, `VANILLA_ICE_CREAM`) so
 * that neither a code change nor a changed Android framework default can silently alter the rule.
 */
@RunWith(RobolectricTestRunner::class)
class ZenRuleFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun silentConditionUri_isUnchanged() {
        // Intentionally a hard-coded literal: changing the constant would orphan existing rules on user devices.
        assertEquals(
            "condition://com.curryplayer.quicksettingssoundprofile/silent_profile_active",
            ZenRuleFactory.SILENT_CONDITION_DND_AND_MODE_URI,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun createDefaultAutomaticZenRule_onAndroidN_hasExpectedValues() {
        val rule = ZenRuleFactory.createDefaultAutomaticZenRule(context)

        verifyCommonRuleFields(rule)
        assertEquals(
            "Owner must be SoundProfileConditionProviderService on API 24-28",
            ComponentName(context, SoundProfileConditionProviderService::class.java),
            rule.owner
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun createDefaultAutomaticZenRule_onAndroidQ_hasExpectedValues() {
        val rule = ZenRuleFactory.createDefaultAutomaticZenRule(context)

        verifyCommonRuleFields(rule)
        assertNull(
            "Owner must be null on API 29-34",
            rule.owner
        )
        assertEquals(
            "Configuration activity must be MainActivity",
            ComponentName(context, MainActivity::class.java),
            rule.configurationActivity
        )
        assertNotNull(
            "ZenPolicy must not be null on API 29-34",
            rule.zenPolicy
        )
        ZenPolicyAssertions.verifyDefaultZenPolicy(rule.zenPolicy!!)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun createDefaultAutomaticZenRule_onAndroidVanillaIceCream_withDefaultIconIndex_hasExpectedValues() {
        val rule = ZenRuleFactory.createDefaultAutomaticZenRule(context)

        verifyVanillaIceCreamRule(rule, IconTheme.VOLUME_DEFAULT)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun createDefaultAutomaticZenRule_onAndroidVanillaIceCream_forEveryIconTheme_hasExpectedValues() {
        IconTheme.entries.forEach { iconTheme ->
            val rule = ZenRuleFactory.createDefaultAutomaticZenRule(context, iconTheme.ordinal)

            verifyVanillaIceCreamRule(rule, iconTheme)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun createDefaultZenPolicy_onAndroidQ_hasExpectedValues() {
        ZenPolicyAssertions.verifyDefaultZenPolicy(ZenRuleFactory.createDefaultZenPolicy())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun createDefaultZenPolicy_onAndroidR_hasExpectedValues() {
        ZenPolicyAssertions.verifyDefaultZenPolicy(ZenRuleFactory.createDefaultZenPolicy())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun createDefaultZenPolicy_onAndroidVanillaIceCream_hasExpectedValues() {
        ZenPolicyAssertions.verifyDefaultZenPolicy(ZenRuleFactory.createDefaultZenPolicy())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun createDefaultZenDeviceEffects_hasExpectedValues() {
        ZenDeviceEffectsAssertions.verifyDefaultZenDeviceEffects(ZenRuleFactory.createDefaultZenDeviceEffects())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun createCondition_onAndroidQ_withStateTrue_hasExpectedValues() {
        verifyCondition(Condition.STATE_TRUE, expectUserActionSource = false)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun createCondition_onAndroidQ_withStateFalse_hasExpectedValues() {
        verifyCondition(Condition.STATE_FALSE, expectUserActionSource = false)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun createCondition_onAndroidVanillaIceCream_withStateTrue_hasExpectedValues() {
        verifyCondition(Condition.STATE_TRUE, expectUserActionSource = true)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun createCondition_onAndroidVanillaIceCream_withStateFalse_hasExpectedValues() {
        verifyCondition(Condition.STATE_FALSE, expectUserActionSource = true)
    }

    private fun verifyCommonRuleFields(rule: AutomaticZenRule) {
        assertEquals(
            "Rule name must match resource string",
            context.getString(R.string.zen_rule_name),
            rule.name
        )
        assertEquals(
            "Condition URI must match SILENT_CONDITION_DND_AND_MODE_URI",
            ZenRuleFactory.SILENT_CONDITION_DND_AND_MODE_URI.toUri(),
            rule.conditionId
        )
        assertEquals(
            "Interruption filter must be PRIORITY",
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            rule.interruptionFilter
        )
        assertTrue("Rule must be enabled", rule.isEnabled)
    }

    private fun verifyVanillaIceCreamRule(rule: AutomaticZenRule, expectedIconTheme: IconTheme) {
        verifyCommonRuleFields(rule)
        assertNull(
            "Owner must be null on API 35+",
            rule.owner
        )
        assertEquals(
            "Configuration activity must be MainActivity",
            ComponentName(context, MainActivity::class.java),
            rule.configurationActivity
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
            rule.iconResId
        )
        assertEquals(
            "Trigger description must match resource string",
            context.getString(R.string.zen_rule_trigger_description),
            rule.triggerDescription
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
    }

    private fun verifyCondition(state: Int, expectUserActionSource: Boolean) {
        val conditionId: Uri = "condition://test/uri".toUri()
        val summary = "Test Summary"
        val condition = ZenRuleFactory.createCondition(conditionId, summary, state)

        assertEquals(
            "Condition ID must match",
            conditionId,
            condition.id
        )
        assertEquals(
            "Condition summary must match",
            summary,
            condition.summary
        )
        assertEquals(
            "Condition state must match",
            state,
            condition.state
        )

        if (expectUserActionSource) {
            assertEquals(
                "Condition source must be SOURCE_USER_ACTION on API 35+",
                Condition.SOURCE_USER_ACTION,
                condition.source
            )
        }
    }
}
