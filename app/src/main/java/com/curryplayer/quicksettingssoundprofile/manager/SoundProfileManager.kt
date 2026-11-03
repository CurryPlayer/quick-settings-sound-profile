package com.curryplayer.quicksettingssoundprofile.manager

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.receivers.RingerModeReceiver
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmScheduler
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmSchedulerImpl
import com.curryplayer.quicksettingssoundprofile.utils.NotificationPolicyUtils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Central manager orchestrating sound profile operations across the application.
 *
 * Coordinates changes between [AudioManager.ringerMode], [android.app.AutomaticZenRule],
 * [DataStoreManager], and [AlarmScheduler], eliminating duplication across UI and receivers.
 *
 * @param context The context used to access system services and resources.
 * @property dataStoreManager Manages persisted user settings and previous profile states.
 * @property alarmScheduler Manages scheduling and cancelling temporary mute timers.
 */
class SoundProfileManager(
    private val context: Context,
    val dataStoreManager: DataStoreManager = DataStoreManager(context),
    val alarmScheduler: AlarmScheduler = AlarmSchedulerImpl(context, dataStoreManager),
) {
    private val appContext: Context = context.applicationContext ?: context
    private val audioManager: AudioManager = appContext.getSystemService(AudioManager::class.java)
    private val notificationManager: NotificationManager = appContext.getSystemService(NotificationManager::class.java)

    private var _cachedRuleId: String = ""

    /**
     * Cold [Flow] emitting the current system ringer mode and subsequent updates.
     */
    val ringerMode: Flow<Int>
        get() = RingerModeReceiver.ringerModeFlow(appContext)

    /**
     * Current system ringer mode ([AudioManager.RINGER_MODE_SILENT],
     * [AudioManager.RINGER_MODE_VIBRATE], or [AudioManager.RINGER_MODE_NORMAL]).
     */
    val currentRingerMode: Int
        get() = audioManager.ringerMode

    /**
     * Checks if notification policy access (Do Not Disturb permission) is granted.
     */
    fun canChangeSoundProfile(): Boolean {
        return NotificationPolicyUtils.isDoNotDisturbPermissionGranted(appContext)
    }

    /**
     * Resolves the [android.app.AutomaticZenRule] ID, ensuring it is cached and exists in the system.
     * If the rule does not exist, it is created and persisted automatically.
     */
    suspend fun resolveZenRuleId(): String {
        if (_cachedRuleId.isNotEmpty() && notificationManager.getAutomaticZenRule(_cachedRuleId) != null) {
            return _cachedRuleId
        }
        val storedId = dataStoreManager.zenRuleId.first()
        if (storedId.isNotEmpty() && notificationManager.getAutomaticZenRule(storedId) != null) {
            _cachedRuleId = storedId
            return _cachedRuleId
        }
        val newId = ZenRuleUtils.syncAutomaticZenRule(appContext, dataStoreManager)
        _cachedRuleId = newId
        return newId
    }

    /**
     * Cycles to the next sound profile in the fixed sequence:
     * NORMAL (2) -> VIBRATE (1) -> SILENT (0) -> NORMAL (2).
     *
     * Cancels any active temporary mute timer and synchronizes the ZenRule.
     *
     * @return The newly applied ringer mode.
     */
    suspend fun cycleToNextMode(): Int {
        val nextMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        setSoundMode(nextMode)
        return nextMode
    }

    /**
     * Sets the desired sound mode directly (NORMAL, VIBRATE, or SILENT).
     *
     * Updates previous mode for temporary mute restoration, cancels active timers,
     * and applies/synchronizes the ZenRule state.
     *
     * @param targetMode The desired ringer mode ([AudioManager.RINGER_MODE_NORMAL],
     * [AudioManager.RINGER_MODE_VIBRATE], or [AudioManager.RINGER_MODE_SILENT]).
     */
    suspend fun setSoundMode(targetMode: Int) {
        alarmScheduler.cancel()
        savePreviousRingerMode(targetMode)

        val ruleId = resolveZenRuleId()
        val activate = (targetMode == AudioManager.RINGER_MODE_SILENT)
        ZenRuleUtils.applyZenRuleAndRingerMode(
            context = appContext,
            ruleId = ruleId,
            activate = activate,
            targetRingerMode = targetMode
        )
    }

    /**
     * Restores the sound mode saved before temporary mute was activated, and clears the timer.
     * Intended for invocation when the temporary mute timer expires.
     */
    suspend fun restorePreviousMode() {
        val previousMode = dataStoreManager.previousRingerMode.first()
        if (previousMode != -1) {
            val ruleId = resolveZenRuleId()
            val activateZenRule = (previousMode == AudioManager.RINGER_MODE_SILENT)
            ZenRuleUtils.applyZenRuleAndRingerMode(
                context = appContext,
                ruleId = ruleId,
                activate = activateZenRule,
                targetRingerMode = previousMode
            )
            dataStoreManager.clearTimer()
        }
    }

    private suspend fun savePreviousRingerMode(targetMode: Int) {
        val currentMode = audioManager.ringerMode
        if (targetMode == AudioManager.RINGER_MODE_SILENT && currentMode != AudioManager.RINGER_MODE_SILENT) {
            dataStoreManager.setPreviousRingerMode(currentMode)
        } else if (targetMode != AudioManager.RINGER_MODE_SILENT) {
            dataStoreManager.setPreviousRingerMode(targetMode)
        }
    }
}
