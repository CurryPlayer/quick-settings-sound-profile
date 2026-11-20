package com.curryplayer.quicksettingssoundprofile.models

import android.media.AudioManager
import com.curryplayer.quicksettingssoundprofile.R
import org.junit.Assert.assertEquals
import org.junit.Test

class IconThemeTest {

    @Test
    fun ordinals_areStableAndComplete() {
        // When new themes are added, they must be added to the end of the list
        // and this expected list must be updated accordingly.
        assertEquals(
            "IconTheme entries order and count must stay stable",
            listOf(
                IconTheme.VOLUME_DEFAULT,
                IconTheme.VOLUME_OUTLINE,
                IconTheme.NOTIFICATIONS,
                IconTheme.NOTIFICATIONS_OUTLINE,
            ),
            IconTheme.entries,
        )
    }

    @Test
    fun iconProperties_matchExpectedDrawables() {
        assertEquals(R.drawable.ic_round_volume_up_24, IconTheme.VOLUME_DEFAULT.ringIcon)
        assertEquals(R.drawable.ic_round_vibration_24, IconTheme.VOLUME_DEFAULT.vibrateIcon)
        assertEquals(R.drawable.ic_round_volume_off_24, IconTheme.VOLUME_DEFAULT.silentIcon)

        assertEquals(R.drawable.ic_round_volume_up_outline_24, IconTheme.VOLUME_OUTLINE.ringIcon)
        assertEquals(R.drawable.ic_round_vibration_outline_24, IconTheme.VOLUME_OUTLINE.vibrateIcon)
        assertEquals(R.drawable.ic_round_volume_off_outline_24, IconTheme.VOLUME_OUTLINE.silentIcon)

        assertEquals(R.drawable.ic_round_notifications_active_24, IconTheme.NOTIFICATIONS.ringIcon)
        assertEquals(R.drawable.ic_round_vibration_24, IconTheme.NOTIFICATIONS.vibrateIcon)
        assertEquals(R.drawable.ic_round_notifications_off_24, IconTheme.NOTIFICATIONS.silentIcon)

        assertEquals(R.drawable.ic_round_notifications_active_outline_24, IconTheme.NOTIFICATIONS_OUTLINE.ringIcon)
        assertEquals(R.drawable.ic_round_vibration_outline_24, IconTheme.NOTIFICATIONS_OUTLINE.vibrateIcon)
        assertEquals(R.drawable.ic_round_notifications_off_outline_24, IconTheme.NOTIFICATIONS_OUTLINE.silentIcon)
    }

    @Test
    fun fromOrdinal_withValidOrdinal_returnsMatchingTheme() {
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(0))
        assertEquals(IconTheme.VOLUME_OUTLINE, IconTheme.fromOrdinal(1))
        assertEquals(IconTheme.NOTIFICATIONS, IconTheme.fromOrdinal(2))
        assertEquals(IconTheme.NOTIFICATIONS_OUTLINE, IconTheme.fromOrdinal(3))
    }

    @Test
    fun fromOrdinal_withInvalidOrdinal_returnsDefaultTheme() {
        // Fallback should be VOLUME_DEFAULT
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(-1))
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(IconTheme.entries.size))
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(999))
    }

    @Test
    fun getIconForMode_withValidRingerMode_returnsMatchingIcon() {
        IconTheme.entries.forEach { theme ->
            assertEquals(
                "${theme.name}: RINGER_MODE_NORMAL should return ringIcon",
                theme.ringIcon,
                theme.getIconForMode(AudioManager.RINGER_MODE_NORMAL),
            )
            assertEquals(
                "${theme.name}: RINGER_MODE_VIBRATE should return vibrateIcon",
                theme.vibrateIcon,
                theme.getIconForMode(AudioManager.RINGER_MODE_VIBRATE),
            )
            assertEquals(
                "${theme.name}: RINGER_MODE_SILENT should return silentIcon",
                theme.silentIcon,
                theme.getIconForMode(AudioManager.RINGER_MODE_SILENT),
            )
        }
    }

    @Test
    fun getIconForMode_withUnknownRingerMode_returnsRingIconFallback() {
        IconTheme.entries.forEach { theme ->
            assertEquals(
                "${theme.name}: unknown ringer mode should fall back to ringIcon",
                theme.ringIcon,
                theme.getIconForMode(-1),
            )
        }
    }
}
