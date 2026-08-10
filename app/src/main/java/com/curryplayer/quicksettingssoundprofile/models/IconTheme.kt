package com.curryplayer.quicksettingssoundprofile.models

import android.media.AudioManager
import com.curryplayer.quicksettingssoundprofile.R

enum class IconTheme(val ringIcon: Int, val vibrateIcon: Int, val silentIcon: Int) {
    // DON'T RE-ARRANGE ORDER ONCE SET! The ordinal is used for the index
    VOLUME_DEFAULT( // ordinal 0
        ringIcon = R.drawable.ic_round_volume_up_24,
        vibrateIcon = R.drawable.ic_round_vibration_24,
        silentIcon = R.drawable.ic_round_volume_off_24
    ),
    NOTIFICATIONS(  // ordinal 1
        ringIcon = R.drawable.ic_round_notifications_active_24,
        vibrateIcon = R.drawable.ic_round_vibration_outline_24,
        silentIcon = R.drawable.ic_round_notifications_off_24
    );

    companion object {
        fun fromOrdinal(ordinal: Int): IconTheme = entries.getOrNull(ordinal) ?: VOLUME_DEFAULT
    }

    fun getIconForMode(ringerMode: Int): Int {
        return when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> ringIcon
            AudioManager.RINGER_MODE_VIBRATE -> vibrateIcon
            AudioManager.RINGER_MODE_SILENT -> silentIcon
            else -> ringIcon
        }
    }

}
