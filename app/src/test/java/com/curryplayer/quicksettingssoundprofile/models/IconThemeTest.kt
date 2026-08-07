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
        assertEquals("NOTIFICATIONS should be at ordinal 1", 1, IconTheme.NOTIFICATIONS.ordinal)
    }

    @Test
    fun testFromOrdinalMapping() {
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(0))
        assertEquals(IconTheme.NOTIFICATIONS, IconTheme.fromOrdinal(1))
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

        // NOTIFICATIONS
        assertEquals(IconTheme.NOTIFICATIONS.ringIcon, IconTheme.NOTIFICATIONS.getIconForMode(AudioManager.RINGER_MODE_NORMAL))
        assertEquals(IconTheme.NOTIFICATIONS.vibrateIcon, IconTheme.NOTIFICATIONS.getIconForMode(AudioManager.RINGER_MODE_VIBRATE))
        assertEquals(IconTheme.NOTIFICATIONS.silentIcon, IconTheme.NOTIFICATIONS.getIconForMode(AudioManager.RINGER_MODE_SILENT))
        assertEquals(IconTheme.NOTIFICATIONS.ringIcon, IconTheme.NOTIFICATIONS.getIconForMode(-1)) // Fallback
    }
}
