package com.curryplayer.quicksettingssoundprofile

import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.pages.RenderSettingsPage
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.Utils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var dndPermissionGrantedState by mutableStateOf(false)
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dataStoreManager = DataStoreManager(this)

        setContent {
            QuickSettingsSoundProfileTheme {
                val isDnDActive by dataStoreManager.activateDnd.collectAsState(initial = false)
                val isMediaMuted by dataStoreManager.muteMedia.collectAsState(initial = false)
                val scope = rememberCoroutineScope()

                RenderSettingsPage(
                    ctx = this,
                    hasPermission = dndPermissionGrantedState,
                    activateDnd = isDnDActive,
                    onActivateDndChange = { newValue ->
                        scope.launch { dataStoreManager.setActivateDnd(newValue) }
                    },
                    muteMedia = isMediaMuted,
                    onMuteMediaChange = { newValue ->
                        scope.launch { dataStoreManager.setMuteMedia(newValue) }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndSyncRule()
    }

    private fun checkPermissionAndSyncRule() {
        dndPermissionGrantedState = Utils.isDoNotDisturbPermissionGranted(this)
        if (dndPermissionGrantedState) {
            lifecycleScope.launch {
                // val muteMedia = dataStoreManager.muteMedia.first()
                syncAutomaticZenRule()
            }
        }
    }

    /**
     * Create the rule if necessary or updates it.
     */
    private suspend fun syncAutomaticZenRule() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        var savedRuleId = dataStoreManager.zenRuleId.first()

        // check if there is already an existing rule with the same name
        if (savedRuleId.isEmpty()) {
            val allRules = notificationManager.automaticZenRules
            val existingRuleEntry = allRules.entries.find { it.value.name == ZenRuleUtils.RULE_NAME }
            if (existingRuleEntry != null) {
                savedRuleId = existingRuleEntry.key
                dataStoreManager.setZenRuleId(savedRuleId)
                Log.i("MainActivity", "Found existing rule: $savedRuleId")
            }
        }

        // check if valid rule exists
        val existingRule = if (savedRuleId.isNotEmpty()) notificationManager.getAutomaticZenRule(savedRuleId) else null

        if (existingRule == null) {
            val newRule = ZenRuleUtils.generateDefaultAutomaticZenRule(this)
            val newId = notificationManager.addAutomaticZenRule(newRule)
            if (newId != null) {
                dataStoreManager.setZenRuleId(newId)
                Log.i("MainActivity", "New ZenRule created: $newId")
            }
        } else {
            // TODO: maybe update the rule here if changes were made to it
            // Unfortunately it is currently not possible to set individual values of a ZenRule while keeping the other values the same
            val updatedRule = existingRule
            val success = notificationManager.updateAutomaticZenRule(savedRuleId, updatedRule)
            Log.i("MainActivity", "ZenRule updated: $success")
        }
    }

}
