package com.example.antimarcipan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Zlatý přechod pro rámečky oken. */
val GoldBorderBrush = Brush.linearGradient(
    listOf(Color(0xFFF6DE9A), Color(0xFFB98A35), Color(0xFFE9C46A), Color(0xFF9C7A2E)),
)

/** Pozadí oken — sytý fialový přechod (místo tmavě šedé). */
private val DialogBackground = Brush.verticalGradient(
    listOf(Color(0xFF34295F), Color(0xFF231C45), Color(0xFF1A1536)),
)

/**
 * Okno ve stylu aplikace: zlatý rámeček, oválné rohy, fialový přechod.
 * Parametry odpovídají AlertDialogu, takže jde snadno zaměnit.
 */
@Composable
fun GoldDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val shape = RoundedCornerShape(28.dp)
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.5.dp, GoldBorderBrush),
            shadowElevation = 12.dp,
        ) {
            Column(
                Modifier.background(DialogBackground).padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                title?.let {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        ProvideTextStyle(MaterialTheme.typography.titleLarge) { it() }
                    }
                }
                text?.let {
                    Box(Modifier.weight(1f, fill = false)) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
