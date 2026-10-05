package com.curryplayer.quicksettingssoundprofile.models

enum class MuteDurationOption {
    MINUTES_30,
    MINUTES_60,
    MINUTES_180,
    CUSTOM;

    companion object {
        fun fromMinutes(minutes: Int): MuteDurationOption {
            return when (minutes) {
                30 -> MINUTES_30
                60 -> MINUTES_60
                180 -> MINUTES_180
                else -> CUSTOM
            }
        }
    }
}
