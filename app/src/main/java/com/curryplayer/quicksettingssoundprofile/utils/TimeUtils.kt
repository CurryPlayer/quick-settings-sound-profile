package com.curryplayer.quicksettingssoundprofile.utils

import android.content.Context
import com.curryplayer.quicksettingssoundprofile.R

object TimeUtils {

    /**
     * Formats the remaining time until [timerEndTime] into a localized, human-readable string.
     *
     * Depending on the remaining duration, the output displays hours, minutes, or a combination of both:
     * - Hours and minutes: e.g., "1 h 30 m" (EN) / "1 Std. 30 Min." (DE)
     * - Hours only: e.g., "2 h" (EN) / "2 Std." (DE)
     * - Minutes only: e.g., "45 m" (EN) / "45 Min." (DE)
     * - Less than a minute / expired: e.g., "0 m" (EN) / "0 Min." (DE)
     *
     * @param context The [Context] used to resolve string resources.
     * @param timerEndTime The target end timestamp in milliseconds (epoch time).
     * @param currentTime The reference current timestamp in milliseconds (defaults to [System.currentTimeMillis]).
     * @return The formatted time string representing the remaining duration.
     */
    fun getFormattedRemainingTime(
        context: Context,
        timerEndTime: Long,
        currentTime: Long = System.currentTimeMillis()
    ): String {
        val remainingMillis = (timerEndTime - currentTime).coerceAtLeast(0L)
        val totalMinutes = (remainingMillis + 59_999L) / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            (hours > 0 && minutes > 0) ->
                context.getString(R.string.time_format_hours_minutes, hours, minutes)

            (hours > 0) ->
                context.getString(R.string.time_format_hours_only, hours)

            else ->
                context.getString(R.string.time_format_minutes_only, minutes)
        }
    }

}
