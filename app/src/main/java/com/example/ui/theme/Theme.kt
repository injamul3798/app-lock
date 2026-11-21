package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppLockLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = EmeraldPrimaryDark,
    secondary = EmeraldLight,
    onSecondary = Color.White,
    secondaryContainer = MintSurface,
    onSecondaryContainer = TextPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SearchSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerSurface,
    onErrorContainer = DangerRed
)

val AppLockShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AppLockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLockLightColorScheme,
        typography = Typography,
        shapes = AppLockShapes,
        content = content
    )
}
