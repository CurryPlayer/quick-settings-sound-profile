package com.curryplayer.quicksettingssoundprofile.manager

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.receivers.RingerModeReceiver
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmScheduler
import com.curryplayer.quicksettingssoundprofile.scheduler.AlarmSchedulerImpl
import com.curryplayer.quicksettingssoundprofile.utils.PermissionUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Central manager orchestrating sound profile operations across the application.
 *
 * Coordinates changes between [AudioManager.ringerMode], [ZenRuleManager],
 * [DataStoreManager], and [AlarmScheduler], eliminating duplication across UI and receivers.
 *
 * @param context The context used to access system services and resources.
 * @property dataStoreManager Manages persisted user settings and previous profile states.
 * @property alarmScheduler Manages scheduling and cancelling temporary mute timers.
 * @property zenRuleManager Manages creation, state, and icon updates of the AutomaticZenRule.
 */
class SoundProfileManager(
    private val context: Context,
    val dataStoreManager: DataStoreManager = DataStoreManager(context),
    val alarmScheduler: AlarmScheduler = AlarmSchedulerImpl(context, dataStoreManager),
    val zenRuleManager: ZenRuleManager = ZenRuleManager(context, dataStoreManager),
) {
    companion object {
        private const val TIMEOUT_ADJUST_NORMAL_MS = 400L
        private const val TIMEOUT_ACTIVATE_SILENT_MS = 400L
        private const val TIMEOUT_DEACTIVATE_FILTER_MS = 250L
        private const val TIMEOUT_SYNCHRONIZE_RINGER_MS = 500L
    }

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
        return PermissionUtils.isDoNotDisturbPermissionGranted(appContext)
    }

    /**
     * Resolves the [android.app.AutomaticZenRule] ID, ensuring it is cached and exists in the system.
     * If the rule does not exist, it is created and persisted automatically.
     */
    suspend fun resolveZenRuleId(): String {
        if (_cachedRuleId.isNotEmpty() && zenRuleManager.getAutomaticZenRule(_cachedRuleId) != null) {
            return _cachedRuleId
        }
        val storedId = dataStoreManager.zenRuleId.first()
        if (storedId.isNotEmpty() && zenRuleManager.getAutomaticZenRule(storedId) != null) {
            _cachedRuleId = storedId
            return _cachedRuleId
        }
        val newId = zenRuleManager.syncAutomaticZenRule()
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
        applyZenRuleAndRingerMode(
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
            applyZenRuleAndRingerMode(
                ruleId = ruleId,
                activate = activateZenRule,
                targetRingerMode = previousMode
            )
            dataStoreManager.clearTimer()
        }
    }

    /**
     * Safely applies the [android.app.AutomaticZenRule] state and ensures the [AudioManager.ringerMode] is synchronized.
     *
     * Due to the asynchronous nature of Android's system services, directly chaining Do Not Disturb (DND)
     * changes with ringer mode changes can cause race conditions (e.g., the `AudioService` ignoring commands
     * or getting stuck in `SILENT` mode).
     *
     * This method resolves these issues by applying the [android.app.AutomaticZenRule] and synchronizing the [AudioManager.ringerMode]
     * reactively using system broadcasts via [RingerModeReceiver] instead of active polling.
     *
     * @param ruleId The unique identifier of the rule to apply.
     * @param activate True if the rule should be activated, false otherwise.
     * @param targetRingerMode The desired ringer mode after activating / deactivating the rule.
     */
    /*
     TODO - Investigation:
        Android 7–10 cause the ring mode to automatically switch to “SILENT” when “VIBRATE” is selected,
        which results in the “Do Not Disturb” schedule not being activated properly.
        Should this still be relevant for this app?
     */
    suspend fun applyZenRuleAndRingerMode(
        ruleId: String,
        activate: Boolean,
        targetRingerMode: Int
    ) {
        if (activate) {
            // Android 17 fix: If current mode is VIBRATE, set ringerMode to 'NORMAL' before activating zenRule
            // to prevent the vibration status bar icon from remaining visible alongside the mode icon.
            val needToAdjustRingerMode = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL &&
                    audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
            if (needToAdjustRingerMode) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                RingerModeReceiver.awaitCondition(appContext, TIMEOUT_ADJUST_NORMAL_MS) {
                    audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
                }
            }

            zenRuleManager.setAutomaticZenRuleState(ruleId, activate = true)

            // Await system transitioning ringerMode to target (SILENT) via the applied ZenRule / priority filter.
            // When ZenRule (on Android15+) or alternatively DND schedule is disabled by user in system settings,
            // this will run in a timeout. Making changes to the ringerMode while a ZenRule / DND schedule is active,
            // will be ignored by the system.
            val isSilent = RingerModeReceiver.awaitCondition(appContext, TIMEOUT_ACTIVATE_SILENT_MS) {
                audioManager.ringerMode == targetRingerMode
            }

            // Fallback: in case the system/OEM does not automatically switch ringerMode to SILENT
            // or DND (ZenRule / schedule) was disabled by the user in system settings
            if (!isSilent && audioManager.ringerMode != targetRingerMode) {
                audioManager.ringerMode = targetRingerMode
            }
        } else {
            // Deactivate the ZenRule
            zenRuleManager.setAutomaticZenRuleState(ruleId, activate = false)

            // Await interruption filter to return to ALL or rule confirmed deactivated (e.g. Android 15+)
            // to avoid timing out if another ZenRule (e.g. Bedtime mode) keeps the interruption filter on PRIORITY.
            RingerModeReceiver.awaitCondition(appContext, TIMEOUT_DEACTIVATE_FILTER_MS) {
                val isFilterAll = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
                val isRuleDeactivated = zenRuleManager.isRuleDeactivated(ruleId)
                isFilterAll || isRuleDeactivated
            }

            // Apply desired ringer mode (NORMAL or VIBRATE)
            audioManager.ringerMode = targetRingerMode

            // Await confirmation of target ringer mode from AudioService
            val isSynchronized = RingerModeReceiver.awaitCondition(appContext, TIMEOUT_SYNCHRONIZE_RINGER_MS) {
                audioManager.ringerMode == targetRingerMode
            }

            // Fallback retry if needed
            if (!isSynchronized && audioManager.ringerMode != targetRingerMode) {
                audioManager.ringerMode = targetRingerMode
            }
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
