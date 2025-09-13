package com.curryplayer.quicksettingssoundprofile

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.curryplayer.quicksettingssoundprofile.pages.RenderSettingsPage
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme
import com.curryplayer.quicksettingssoundprofile.utils.Utils

class MainActivity : ComponentActivity() {

    private var dndPermissionGrantedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate")
        enableEdgeToEdge()

        dndPermissionGrantedState.value = Utils.isDoNotDisturbPermissionGranted(this)

        setAppContent(this, dndPermissionGrantedState.value)
    }

    override fun onResume() {
        super.onResume()
        Log.i("MainActivity", "onResume")

        dndPermissionGrantedState.value = Utils.isDoNotDisturbPermissionGranted(this)

        setAppContent(this, dndPermissionGrantedState.value)
    }

    override fun onStart() {
        super.onStart()
        Log.i("MainActivity", "onStart")
    }

    private fun setAppContent(ctx: Context, hasPermission: Boolean) {
        setContent {
            QuickSettingsSoundProfileTheme {
                RenderSettingsPage(ctx, hasPermission)
            }
        }
    }
}
