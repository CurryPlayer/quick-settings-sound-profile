package com.curryplayer.quicksettingssoundprofile.models

import org.junit.Assert.assertEquals
import org.junit.Test

class MuteDurationOptionTest {

    @Test
    fun ensureMuteDurationOptionOrdinalsAreStable() {
        // When new options are added, they must be added to the end of the list
        // and a corresponding check must be added here.
        assertEquals("MINUTES_30 should be at ordinal 0", 0, MuteDurationOption.MINUTES_30.ordinal)
        assertEquals("MINUTES_60 should be at ordinal 1", 1, MuteDurationOption.MINUTES_60.ordinal)
        assertEquals(
            "MINUTES_180 should be at ordinal 2",
            2,
            MuteDurationOption.MINUTES_180.ordinal
        )
        assertEquals("CUSTOM should be at ordinal 3", 3, MuteDurationOption.CUSTOM.ordinal)
    }

    @Test
    fun testFromOrdinalMapping() {
        assertEquals(MuteDurationOption.MINUTES_30, MuteDurationOption.fromOrdinal(0))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(1))
        assertEquals(MuteDurationOption.MINUTES_180, MuteDurationOption.fromOrdinal(2))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromOrdinal(3))
    }

    @Test
    fun testFromOrdinalFallback() {
        // Fallback should be MINUTES_60
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(-1))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(999))
    }

    @Test
    fun testFromMinutesMapping() {
        assertEquals(MuteDurationOption.MINUTES_30, MuteDurationOption.fromMinutes(30))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromMinutes(60))
        assertEquals(MuteDurationOption.MINUTES_180, MuteDurationOption.fromMinutes(180))
    }

    @Test
    fun testFromMinutesFallback() {
        // Fallback should be CUSTOM
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(0))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(45))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(-1))
    }

    @Test
    fun testMinutesMapping() {
        assertEquals(30, MuteDurationOption.MINUTES_30.minutes)
        assertEquals(60, MuteDurationOption.MINUTES_60.minutes)
        assertEquals(180, MuteDurationOption.MINUTES_180.minutes)
        assertEquals("CUSTOM should always return 42 (the answer to everything :)", 42, MuteDurationOption.CUSTOM.minutes)
    }
}
