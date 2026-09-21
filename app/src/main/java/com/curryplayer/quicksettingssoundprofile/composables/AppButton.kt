package com.curryplayer.quicksettingssoundprofile.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class AppButtonType {
    FILLED,
    OUTLINED
}

// TODO: unify all buttons into one class to achieve consistency
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AppButtonType = AppButtonType.FILLED,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(50.dp) // Runde Ecken (Pill-Form / Stadium Shape)
    
    if (type == AppButtonType.OUTLINED) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AppButtonType = AppButtonType.FILLED,
    enabled: Boolean = true
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        type = type,
        enabled = enabled
    ) {
        Text(text = text)
    }
}
