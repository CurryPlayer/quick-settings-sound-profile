package com.curryplayer.quicksettingssoundprofile.utils

import android.content.Context
import android.text.format.DateFormat
import java.util.Date
import com.curryplayer.quicksettingssoundprofile.R

object TimeUtils {

    /**
     * Formats a duration in minutes into a localized, human-readable string.
     *
     * Depending on the duration, the output displays hours, minutes, or a combination of both:
     * - Hours and minutes: e.g., "1 h 30 m" (EN) / "1 Std. 30 Min." (DE)
     * - Hours only: e.g., "2 h" (EN) / "2 Std." (DE)
     * - Minutes only: e.g., "45 m" (EN) / "45 Min." (DE)
     * - Less than a minute / zero: e.g., "0 m" (EN) / "0 Min." (DE)
     *
     * @param context The [Context] used to resolve string resources.
     * @param totalMinutes The total duration in minutes.
     * @return The formatted duration string.
     */
    fun getFormattedDuration(
        context: Context,
        totalMinutes: Long,
    ): String {
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L

        return when {
            (hours > 0L) && (minutes > 0L) ->
                context.getString(R.string.time_format_hours_minutes, hours, minutes)

            hours > 0L ->
                context.getString(R.string.time_format_hours_only, hours)

            else ->
                context.getString(R.string.time_format_minutes_only, minutes)
        }
    }

    /**
     * Formats a duration in minutes into a localized, human-readable string.
     */
    fun getFormattedDuration(
        context: Context,
        totalMinutes: Int,
    ): String = getFormattedDuration(context, totalMinutes.toLong())

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
        currentTime: Long = System.currentTimeMillis(),
    ): String {
        val remainingMillis = (timerEndTime - currentTime).coerceAtLeast(0L)
        val totalMinutes = (remainingMillis + 59_999L) / 60_000L
        return getFormattedDuration(context, totalMinutes)
    }


    /**
     * Formats the wall-clock time at which the timer ends ([timerEndTime]) as a localized time of day.
     *
     * Uses the system time format and time zone, so it respects the user's 12/24 hour setting,
     * e.g. "12:49" (24 h) or "11:35 AM" (12 h, EN).
     *
     * @param context The [Context] used to resolve the system time format.
     * @param timerEndTime The target end timestamp in milliseconds (epoch time).
     * @return The formatted time of day.
     */
    fun getFormattedEndTime(
        context: Context,
        timerEndTime: Long,
    ): String = DateFormat.getTimeFormat(context).format(Date(timerEndTime))
}
