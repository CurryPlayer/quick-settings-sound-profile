package com.curryplayer.quicksettingssoundprofile.models

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
        // Fallback sollte VOLUME_DEFAULT sein
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(-1))
        assertEquals(IconTheme.VOLUME_DEFAULT, IconTheme.fromOrdinal(999))
    }
}
