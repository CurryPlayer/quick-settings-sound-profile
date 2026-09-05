package com.curryplayer.quicksettingssoundprofile.composables

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R
import com.curryplayer.quicksettingssoundprofile.models.IconTheme
import com.curryplayer.quicksettingssoundprofile.services.SoundProfileTileService

@Composable
fun RenderAllSetCard(
    ctx: Context,
    iconTheme: IconTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_round_check_circle_24),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = ctx.getString(R.string.all_set),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ctx.getString(R.string.tile_placement_manual),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(24.dp))

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
