package com.curryplayer.quicksettingssoundprofile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.curryplayer.quicksettingssoundprofile.data.DataStoreManager
import com.curryplayer.quicksettingssoundprofile.pages.RenderSettingsPage
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.NotificationPolicyUtils
import com.curryplayer.quicksettingssoundprofile.utils.ZenRuleUtils
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
                val savedZenRuleId by dataStoreManager.zenRuleId.collectAsState(initial = "")

                RenderSettingsPage(
                    ctx = this,
                    hasPermission = dndPermissionGrantedState,
                    ruleId = savedZenRuleId
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndSyncRule()
    }

    private fun checkPermissionAndSyncRule() {
        dndPermissionGrantedState = NotificationPolicyUtils.isDoNotDisturbPermissionGranted(this)
        if (dndPermissionGrantedState) {
            lifecycleScope.launch {
                ZenRuleUtils.syncAutomaticZenRule(this@MainActivity, dataStoreManager)
            }
        }
    }
}
