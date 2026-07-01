package com.ieum.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val IeumColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = BubbleSentText,
    primaryContainer = CoralSoft,
    onPrimaryContainer = CoralDark,
    secondary = Sage,
    onSecondary = BubbleSentText,
    secondaryContainer = SageSoft,
    onSecondaryContainer = SageDark,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = CardBorder,
    onSurfaceVariant = InkSub,
    outline = MutedSoft,
    outlineVariant = Line,
)

@Composable
fun IeumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IeumColorScheme,
        typography = Typography,
        content = content
    )
}
