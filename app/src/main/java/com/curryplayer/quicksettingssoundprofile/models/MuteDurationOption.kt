package com.curryplayer.quicksettingssoundprofile.models

enum class MuteDurationOption(val minutes: Int) {
    // The ordinal is used for the index
    MINUTES_30(30),     // ordinal 0
    MINUTES_60(60),     // ordinal 1, should be considered as default
    MINUTES_180(180),   // ordinal 2
    CUSTOM(42);         // ordinal 3, answer to everything :)

    companion object {
        fun fromOrdinal(ordinal: Int): MuteDurationOption = entries.getOrNull(ordinal) ?: MINUTES_60

        fun fromMinutes(minutes: Int): MuteDurationOption {
            return when (minutes) {
                MINUTES_30.minutes -> MINUTES_30
                MINUTES_60.minutes -> MINUTES_60
                MINUTES_180.minutes -> MINUTES_180
                else -> CUSTOM
            }
        }
    }
}
