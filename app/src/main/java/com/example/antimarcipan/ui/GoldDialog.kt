package com.example.antimarcipan.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

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
                    Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
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

private val GoldChipBrush = Brush.linearGradient(listOf(Color(0xFFF6DE9A), Color(0xFFD9B05A), Color(0xFFB98A35)))

/** Přepínací čip ve zlaté: vybraný = zlatý podklad s tmavým textem, ostatní zlatý rámeček. */
@Composable
fun GoldChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = Color(0xFFEAD9A8),
            iconColor = Color(0xFFEAD9A8),
            selectedContainerColor = Color(0xFFE2BC62),
            selectedLabelColor = Color(0xFF1E1633),
            selectedLeadingIconColor = Color(0xFF1E1633),
        ),
        border = if (selected) null else BorderStroke(1.dp, GoldChipBrush),
    )
}
