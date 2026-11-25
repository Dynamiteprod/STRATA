package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StrataDarkColorScheme = darkColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TextPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TextPrimary,
    secondary = KoraGold,
    onSecondary = ObsidianBg,
    secondaryContainer = KoraGoldContainer,
    onSecondaryContainer = KoraGold,
    tertiary = LaguneTeal,
    onTertiary = ObsidianBg,
    tertiaryContainer = LaguneTealContainer,
    onTertiaryContainer = LaguneTeal,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceHigh,
    surfaceContainerHigh = StudioSurfaceBright,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.55f),
    error = ErrorRed,
    onError = ObsidianBg
)

val StrataShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StrataTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StrataDarkColorScheme,
        typography = StrataTypography,
        shapes = StrataShapes,
        content = content
    )
}
