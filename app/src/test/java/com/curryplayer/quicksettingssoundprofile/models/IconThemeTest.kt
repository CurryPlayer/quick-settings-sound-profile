package com.curryplayer.quicksettingssoundprofile.models

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Test

class IconThemeTest {

    @Test
    fun ensureIconThemeOrdinalsAreStable() {
        // When new themes are added, they must be added to the end of the list
        // and a corresponding check must be added here.
        assertEquals("VOLUME_DEFAULT should be at ordinal 0", 0, IconTheme.VOLUME_DEFAULT.ordinal)
        assertEquals("VOLUME_OUTLINE should be at ordinal 0", 1, IconTheme.VOLUME_OUTLINE.ordinal)
        assertEquals("NOTIFICATIONS should be at ordinal 1", 2, IconTheme.NOTIFICATIONS.ordinal)
        assertEquals("NOTIFICATIONS_OUTLINE should be at ordinal 1", 3, IconTheme.NOTIFICATIONS_OUTLINE.ordinal)
    }

    @Test
    fun testFromOrdinalMapping() {
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(0))
        assertEquals(IconTheme.VOLUME_OUTLINE, IconTheme.fromOrdinal(1))
        assertEquals(IconTheme.NOTIFICATIONS, IconTheme.fromOrdinal(2))
        assertEquals(IconTheme.NOTIFICATIONS_OUTLINE, IconTheme.fromOrdinal(3))
    }

    @Test
    fun testFromOrdinalFallback() {
        // Fallback should be VOLUME_DEFAULT
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(-1))
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(999))
    }

    @Test
    fun testGetIconForMode() {
        // VOLUME_DEFAULT
        assertEquals(IconTheme.VOLUME_DEFAULT.ringIcon, IconTheme.VOLUME_DEFAULT.getIconForMode(AudioManager.RINGER_MODE_NORMAL))
        assertEquals(IconTheme.VOLUME_DEFAULT.vibrateIcon, IconTheme.VOLUME_DEFAULT.getIconForMode(AudioManager.RINGER_MODE_VIBRATE))
        assertEquals(IconTheme.VOLUME_DEFAULT.silentIcon, IconTheme.VOLUME_DEFAULT.getIconForMode(AudioManager.RINGER_MODE_SILENT))
        assertEquals(IconTheme.VOLUME_DEFAULT.ringIcon, IconTheme.VOLUME_DEFAULT.getIconForMode(-1)) // Fallback

        // VOLUME_OUTLINE
        assertEquals(IconTheme.VOLUME_OUTLINE.ringIcon, IconTheme.VOLUME_OUTLINE.getIconForMode(AudioManager.RINGER_MODE_NORMAL))
        assertEquals(IconTheme.VOLUME_OUTLINE.vibrateIcon, IconTheme.VOLUME_OUTLINE.getIconForMode(AudioManager.RINGER_MODE_VIBRATE))
        assertEquals(IconTheme.VOLUME_OUTLINE.silentIcon, IconTheme.VOLUME_OUTLINE.getIconForMode(AudioManager.RINGER_MODE_SILENT))
        assertEquals(IconTheme.VOLUME_OUTLINE.ringIcon, IconTheme.VOLUME_OUTLINE.getIconForMode(-1)) // Fallback

        // NOTIFICATIONS
        assertEquals(IconTheme.NOTIFICATIONS.ringIcon, IconTheme.NOTIFICATIONS.getIconForMode(AudioManager.RINGER_MODE_NORMAL))
        assertEquals(IconTheme.NOTIFICATIONS.vibrateIcon, IconTheme.NOTIFICATIONS.getIconForMode(AudioManager.RINGER_MODE_VIBRATE))
        assertEquals(IconTheme.NOTIFICATIONS.silentIcon, IconTheme.NOTIFICATIONS.getIconForMode(AudioManager.RINGER_MODE_SILENT))
        assertEquals(IconTheme.NOTIFICATIONS.ringIcon, IconTheme.NOTIFICATIONS.getIconForMode(-1)) // Fallback

        // NOTIFICATIONS_OUTLINE
        assertEquals(IconTheme.NOTIFICATIONS_OUTLINE.ringIcon, IconTheme.NOTIFICATIONS_OUTLINE.getIconForMode(AudioManager.RINGER_MODE_NORMAL))
        assertEquals(IconTheme.NOTIFICATIONS_OUTLINE.vibrateIcon, IconTheme.NOTIFICATIONS_OUTLINE.getIconForMode(AudioManager.RINGER_MODE_VIBRATE))
        assertEquals(IconTheme.NOTIFICATIONS_OUTLINE.silentIcon, IconTheme.NOTIFICATIONS_OUTLINE.getIconForMode(AudioManager.RINGER_MODE_SILENT))
        assertEquals(IconTheme.NOTIFICATIONS_OUTLINE.ringIcon, IconTheme.NOTIFICATIONS_OUTLINE.getIconForMode(-1)) // Fallback
    }
}
