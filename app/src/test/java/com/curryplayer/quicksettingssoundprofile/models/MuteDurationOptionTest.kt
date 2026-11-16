package com.curryplayer.quicksettingssoundprofile.models

import org.junit.Assert.assertEquals
import org.junit.Test

class MuteDurationOptionTest {

    @Test
    fun ordinals_areStableAndComplete() {
        // When new options are added, they must be added to the end of the list
        // and this expected list must be updated accordingly.
        assertEquals(
            "MuteDurationOption entries order and count must stay stable",
            listOf(
                MuteDurationOption.MINUTES_30,
                MuteDurationOption.MINUTES_60,
                MuteDurationOption.MINUTES_180,
                MuteDurationOption.CUSTOM,
            ),
            MuteDurationOption.entries,
        )
    }

    @Test
    fun minutes_matchExpectedValues() {
        assertEquals(30, MuteDurationOption.MINUTES_30.minutes)
        assertEquals(60, MuteDurationOption.MINUTES_60.minutes)
        assertEquals(180, MuteDurationOption.MINUTES_180.minutes)
        assertEquals(
            "CUSTOM should always return 42 (the answer to everything :)",
            42,
            MuteDurationOption.CUSTOM.minutes,
        )
    }

    @Test
    fun fromOrdinal_withValidOrdinal_returnsMatchingOption() {
        assertEquals(MuteDurationOption.MINUTES_30, MuteDurationOption.fromOrdinal(0))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(1))
        assertEquals(MuteDurationOption.MINUTES_180, MuteDurationOption.fromOrdinal(2))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromOrdinal(3))
    }

    @Test
    fun fromOrdinal_withInvalidOrdinal_returnsDefaultOption() {
        // Fallback should be MINUTES_60
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(-1))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(MuteDurationOption.entries.size))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromOrdinal(999))
    }

    @Test
    fun fromMinutes_withKnownMinutes_returnsMatchingOption() {
        assertEquals(MuteDurationOption.MINUTES_30, MuteDurationOption.fromMinutes(30))
        assertEquals(MuteDurationOption.MINUTES_60, MuteDurationOption.fromMinutes(60))
        assertEquals(MuteDurationOption.MINUTES_180, MuteDurationOption.fromMinutes(180))
    }

    @Test
    fun fromMinutes_withUnknownMinutes_returnsCustomFallback() {
        // Fallback should be CUSTOM
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(0))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(MuteDurationOption.CUSTOM.minutes))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(45))
        assertEquals(MuteDurationOption.CUSTOM, MuteDurationOption.fromMinutes(-1))
    }
}
