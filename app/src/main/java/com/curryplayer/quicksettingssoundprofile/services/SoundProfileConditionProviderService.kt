package com.curryplayer.quicksettingssoundprofile.services

import android.service.notification.ConditionProviderService
import android.net.Uri

/*
This deprecated service is required for Android versions older than 10 to
enable the addition of an AutomaticZenRule via a ConditionProviderService.
It ensures backward compatibility despite its deprecated status.
 */
class SoundProfileConditionProviderService : ConditionProviderService() {
    override fun onConnected() {}
    override fun onSubscribe(conditionId: Uri?) {}
    override fun onUnsubscribe(conditionId: Uri?) {}
}