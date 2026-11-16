package com.curryplayer.quicksettingssoundprofile.factory

import android.os.Build
import android.service.notification.ZenDeviceEffects
import androidx.annotation.RequiresApi
import org.junit.Assert.assertFalse

/**
 * Assertions for [ZenDeviceEffects] (API 35+).
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
object ZenDeviceEffectsAssertions {

    /** Every public getter of [ZenDeviceEffects] is asserted, so no effect can silently become active. */
    fun verifyDefaultZenDeviceEffects(deviceEffects: ZenDeviceEffects) {
        assertFalse(
            "Dim wallpaper must be false",
            deviceEffects.shouldDimWallpaper()
        )
        assertFalse(
            "Night mode must be false",
            deviceEffects.shouldUseNightMode()
        )
        assertFalse(
            "Grayscale must be false",
            deviceEffects.shouldDisplayGrayscale()
        )
        assertFalse(
            "Suppress ambient display must be false",
            deviceEffects.shouldSuppressAmbientDisplay()
        )
    }
}
