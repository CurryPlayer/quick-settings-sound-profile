package com.curryplayer.quicksettingssoundprofile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickSettingsSoundProfileTheme {
                QuickSettingsSoundProfileApp()
            }
        }
    }
}

@Composable
fun QuickSettingsSoundProfileApp(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.DarkGray),
        topBar = { RenderTopAppBar() },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
            ) {
                NewCategoryName("Mute Settings")
                SettingsToggleItem(
                    "Activate 'Do not disturb' mode on mute",
                    "If enabled, 'Do not disturb' mode will be activated when the device is muted",
                    false
                )
                SettingsToggleItem(
                    "Also mute media",
                    "If enabled, media will be muted when the device is muted",
                    false
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTopAppBar() {
    TopAppBar(
        title = { Text("Quick Settings Sound Profile") }
    )
}

@Composable
private fun NewCategoryName(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(16.dp)
    )
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(color = Color.LightGray)
    )
}

@Composable
private fun SettingsToggleItem(title: String, subtitle: String?, initialChecked: Boolean) {
    val checkedState: MutableState<Boolean> = remember { mutableStateOf(initialChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 20.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        Switch(
            checked = checkedState.value,
            onCheckedChange = { newValue -> checkedState.value = newValue },
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuickSettingsSoundProfilePreview() {
    QuickSettingsSoundProfileTheme {
        QuickSettingsSoundProfileApp()
    }
}