package com.curryplayer.quicksettingssoundprofile.models

enum class MuteDurationOption(val minutes: Int) {
    MINUTES_30(30),
    MINUTES_60(60), // should be considered as default
    MINUTES_180(180),
    CUSTOM(0);

    companion object {
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
