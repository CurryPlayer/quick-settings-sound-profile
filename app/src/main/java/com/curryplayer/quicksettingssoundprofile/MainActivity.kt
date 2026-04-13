package com.curryplayer.quicksettingssoundprofile

import android.os.Bundle
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
                ZenRuleUtils.syncAutomaticZenRule(this@MainActivity, dataStoreManager)
            }
        }
    }
}
