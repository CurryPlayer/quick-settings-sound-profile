package com.curryplayer.quicksettingssoundprofile.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [BroadcastReceiver] that monitors changes to the system ringer mode ([AudioManager.RINGER_MODE_CHANGED_ACTION])
 * as well as Do Not Disturb / Android Modes ([NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED]).
 *
 * Listening to both actions is required because activating Do Not Disturb or an [android.app.AutomaticZenRule]
 * with an interruption filter can alter the effective ringer behavior without always broadcasting a standard
 * ringer mode change alone.
 *
 * Triggers the [onRingerModeChanged] callback whenever either of these states changes, and provides helper
 * methods to safely [register] and [unregister] the receiver (including Android 13+ export safety).
 *
 * @param context The [Context] used for registering and unregistering the receiver.
 * @property onRingerModeChanged Callback invoked when the ringer mode or interruption filter changes.
 */
class RingerModeReceiver(
    private val context: Context,
    private val onRingerModeChanged: () -> Unit,
) : BroadcastReceiver() {

    private val appContext = context.applicationContext ?: context
    private val isReceiverRegistered = AtomicBoolean(false)

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if ((action == AudioManager.RINGER_MODE_CHANGED_ACTION) ||
            (action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        ) {
            onRingerModeChanged()
        }
    }

    fun register() {
        if (isReceiverRegistered.compareAndSet(false, true)) {
            val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION).apply {
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(this, filter)
            }
        }
    }

    fun unregister() {
        if (isReceiverRegistered.compareAndSet(true, false)) {
            try {
                appContext.unregisterReceiver(this)
            } catch (_: IllegalArgumentException) {
                // Receiver previously not registered or already unregistered
            }
        }
    }

    companion object {
        /**
         * Returns a cold [Flow] that emits the current [AudioManager.ringerMode] immediately upon collection,
         * and subsequently whenever [AudioManager.RINGER_MODE_CHANGED_ACTION] or
         * [NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED] occurs.
         *
         * The receiver is registered when collection begins and cleanly unregistered when collection stops.
         *
         * @param context The context used to register the receiver and access system services.
         * @return A [Flow] emitting the current ringer mode.
         */
        fun ringerModeFlow(context: Context): Flow<Int> = callbackFlow {
            val appContext = context.applicationContext ?: context
            val audioManager = appContext.getSystemService(AudioManager::class.java)

            val receiver = RingerModeReceiver(appContext) {
                trySend(audioManager.ringerMode)
            }
            receiver.register()

            // Emit current mode immediately on collection
            trySend(audioManager.ringerMode)

            awaitClose {
                receiver.unregister()
            }
        }.distinctUntilChanged()

        /**
         * Suspends until the specified [predicate] evaluates to true, triggered by events from
         * [RingerModeReceiver], or until [timeoutMs] elapses.
         *
         * @param context The context used to register the receiver.
         * @param timeoutMs The maximum time to wait in milliseconds.
         * @param predicate The condition to check on each broadcast event.
         * @return True if the condition was met before the timeout, false otherwise.
         */
        suspend fun awaitCondition(
            context: Context,
            timeoutMs: Long,
            predicate: () -> Boolean
        ): Boolean {
            if (predicate()) return true

            return withTimeoutOrNull(timeoutMs.milliseconds) {
                suspendCancellableCoroutine { continuation ->
                    val isResumed = AtomicBoolean(false)
                    lateinit var receiver: RingerModeReceiver

                    receiver = RingerModeReceiver(context) {
                        if (predicate() && isResumed.compareAndSet(false, true)) {
                            receiver.unregister()
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        }
                    }

                    receiver.register()

                    continuation.invokeOnCancellation {
                        receiver.unregister()
                    }

                    // Check again in case condition was fulfilled during receiver registration
                    if (predicate() && isResumed.compareAndSet(false, true)) {
                        receiver.unregister()
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }
                }
            } ?: false
        }
    }
}
