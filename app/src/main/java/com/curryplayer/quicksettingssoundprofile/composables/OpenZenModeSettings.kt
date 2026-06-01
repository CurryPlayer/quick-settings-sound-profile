package com.curryplayer.quicksettingssoundprofile.composables

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curryplayer.quicksettingssoundprofile.R

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RenderOpenZenModeSettings(
    ctx: Context,
    ruleId: String,
    hasPermission: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    openZenRuleSettings(ctx, ruleId)
                },
                enabled = hasPermission && ruleId.isNotEmpty(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = ctx.getString(R.string.open_mode_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.open_mode_grant_permission_first),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontStyle = FontStyle.Italic,
                )
            }

        }
    }
}

// requires Android 8 / API 26
@RequiresApi(Build.VERSION_CODES.O)
private fun openZenRuleSettings(context: Context, ruleId: String) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        buildIntentForAndroidVanillaIceCreamAndAbove(context, ruleId)
    } else {
        Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.toast_intent_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun buildIntentForAndroidVanillaIceCreamAndAbove(context: Context, ruleId: String): Intent {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // a matching Activity will only be found if NotificationManager.areAutomaticZenRulesUserManaged() is true.
    return if (notificationManager.areAutomaticZenRulesUserManaged()) {
        Intent(Settings.ACTION_AUTOMATIC_ZEN_RULE_SETTINGS).apply {
            putExtra(Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID, ruleId)
        }
    } else {
        Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
    }
}
