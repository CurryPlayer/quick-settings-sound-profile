package com.curryplayer.quicksettingssoundprofile.factory

import android.os.Build
import android.service.notification.ZenPolicy
import androidx.annotation.RequiresApi
import org.junit.Assert.assertEquals

/**
 * Assertions for [ZenPolicy] (API 29+).
 */
@RequiresApi(Build.VERSION_CODES.Q)
object ZenPolicyAssertions {

    /** Every getter of [ZenPolicy] is asserted, so no field can stay [ZenPolicy.STATE_UNSET]. */
    fun verifyDefaultZenPolicy(zenPolicy: ZenPolicy) {
        // Priority categories
        assertEquals(
            "Alarms must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.priorityCategoryAlarms
        )
        assertEquals(
            "Media must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.priorityCategoryMedia
        )
        assertEquals(
            "System sounds must be DISALLOWED",
            ZenPolicy.STATE_DISALLOW,
            zenPolicy.priorityCategorySystem
        )
        assertEquals(
            "Reminders must be DISALLOWED",
            ZenPolicy.STATE_DISALLOW,
            zenPolicy.priorityCategoryReminders
        )
        assertEquals(
            "Events must be DISALLOWED",
            ZenPolicy.STATE_DISALLOW,
            zenPolicy.priorityCategoryEvents
        )
        assertEquals(
            "Calls must be DISALLOWED",
            ZenPolicy.STATE_DISALLOW,
            zenPolicy.priorityCategoryCalls
        )
        assertEquals(
            "Messages must be DISALLOWED",
            ZenPolicy.STATE_DISALLOW,
            zenPolicy.priorityCategoryMessages
        )
        assertEquals(
            "Repeat callers must be DISALLOWED",
            ZenPolicy.STATE_DISALLOW,
            zenPolicy.priorityCategoryRepeatCallers,
        )

        // Senders
        assertEquals(
            "Call senders must be NONE",
            ZenPolicy.PEOPLE_TYPE_NONE,
            zenPolicy.priorityCallSenders
        )
        assertEquals(
            "Message senders must be NONE",
            ZenPolicy.PEOPLE_TYPE_NONE,
            zenPolicy.priorityMessageSenders
        )

        // Visual effects
        assertEquals(
            "Full screen intent must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectFullScreenIntent,
        )
        assertEquals(
            "Lights must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectLights
        )
        assertEquals(
            "Peeking must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectPeek
        )
        assertEquals(
            "Status bar icons must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectStatusBar
        )
        assertEquals(
            "Badges must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectBadge
        )
        assertEquals(
            "Ambient display must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectAmbient
        )
        assertEquals(
            "Notification list must be ALLOWED",
            ZenPolicy.STATE_ALLOW,
            zenPolicy.visualEffectNotificationList,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertEquals(
                "Conversations must be DISALLOWED",
                ZenPolicy.STATE_DISALLOW,
                zenPolicy.priorityCategoryConversations,
            )
            assertEquals(
                "Conversation senders must be NONE",
                ZenPolicy.CONVERSATION_SENDERS_NONE,
                zenPolicy.priorityConversationSenders,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            assertEquals(
                "Priority channels must be DISALLOWED",
                ZenPolicy.STATE_DISALLOW,
                zenPolicy.priorityChannelsAllowed,
            )
        }
    }
}
