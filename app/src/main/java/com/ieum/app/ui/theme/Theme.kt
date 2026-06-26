package com.ieum.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WarmColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = BubbleSentText,
    primaryContainer = CoralContainer,
    onPrimaryContainer = OnCoralContainer,
    secondary = WarmBrown,
    onSecondary = WarmSurface,
    secondaryContainer = WarmBrownContainer,
    onSecondaryContainer = OnWarmBrownContainer,
    background = WarmBackground,
    onBackground = BubbleReceivedText,
    surface = WarmSurface,
    onSurface = BubbleReceivedText,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline,
)

@Composable
fun IeumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmColorScheme,
        typography = Typography,
        content = content
    )
}
