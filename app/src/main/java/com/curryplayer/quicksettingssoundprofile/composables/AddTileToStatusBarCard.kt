package com.curryplayer.quicksettingssoundprofile.composables

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileTileService
import com.curryplayer.quicksettingssoundprofile.ui.theme.QuickSettingsSoundProfileTheme

@Composable
fun RenderAddTileToStatusBarCard(
    ctx: Context,
    iconTheme: IconTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = ctx.getString(R.string.tile_placement_manual),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        addTileToStatusBar(ctx, iconTheme)
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = ctx.getString(R.string.button_add_tile),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddTileToStatusBarCardPreview() {
    QuickSettingsSoundProfileTheme {
        RenderAddTileToStatusBarCard(
            ctx = LocalContext.current,
            iconTheme = IconTheme.VOLUME_DEFAULT
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun addTileToStatusBar(ctx: Context, iconTheme: IconTheme) {
    val componentName = ComponentName(ctx, SoundProfileTileService::class.java)
    val statusBarManager = ctx.getSystemService(StatusBarManager::class.java)
    val icon = Icon.createWithResource(ctx, iconTheme.ringIcon)
    statusBarManager.requestAddTileService(
        componentName,
        ctx.getString(R.string.profile_sound_label),
        icon,
        ctx.mainExecutor,
    ) { result ->
        when (result) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.tile_added_success),
                    Toast.LENGTH_SHORT
                ).show()
            }

            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.tile_already_added),
                    Toast.LENGTH_SHORT
                ).show()
            }

            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.tile_not_added),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }
}
