package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ArtisanalLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = ClayPeachContainer,
    onPrimaryContainer = DeepTerracottaOnContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = WarmMintContainer,
    onSecondaryContainer = DeepSageOnContainer,
    tertiary = HoneyTertiary,
    onTertiary = HoneyOnTertiary,
    tertiaryContainer = SoftHoneyContainer,
    onTertiaryContainer = DeepHoneyOnContainer,
    background = CreamBackground,
    onBackground = EspressoInk,
    surface = IvorySurface,
    onSurface = EspressoInk,
    surfaceVariant = WarmOatContainer,
    onSurfaceVariant = RoastedBrownMuted,
    surfaceContainerLowest = IvorySurface,
    surfaceContainerLow = SoftLinenLow,
    surfaceContainer = WarmOatContainer,
    surfaceContainerHigh = WarmSandHigh,
    surfaceContainerHighest = WarmSandHighest,
    outline = WarmOutline,
    outlineVariant = WarmOutlineSubtle,
    error = WarmCrimsonError,
    onError = TerracottaOnPrimary,
    errorContainer = WarmCrimsonContainer,
    onErrorContainer = OnWarmCrimsonContainer
)

private val ArtisanalDarkColorScheme = darkColorScheme(
    primary = DarkTerracottaPrimary,
    onPrimary = DarkOnTerracotta,
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = DarkPeachOnContainer,
    secondary = DarkSageSecondary,
    onSecondary = DarkOnSage,
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = DarkMintOnContainer,
    tertiary = DarkHoneyTertiary,
    onTertiary = DarkOnHoney,
    tertiaryContainer = DarkHoneyContainer,
    onTertiaryContainer = DarkSoftHoneyOnContainer,
    background = DarkEspressoBackground,
    onBackground = DarkCreamInk,
    surface = DarkRoastSurface,
    onSurface = DarkCreamInk,
    surfaceVariant = DarkCocoaContainer,
    onSurfaceVariant = DarkWarmMuted,
    surfaceContainerLowest = DarkEspressoBackground,
    surfaceContainerLow = DarkRoastSurface,
    surfaceContainer = DarkCocoaContainer,
    surfaceContainerHigh = Color(0xFF3A2D26),
    surfaceContainerHighest = Color(0xFF473830),
    outline = DarkWarmOutline,
    outlineVariant = Color(0xFF3D3028),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

private val ArtisanalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LasCuentasDePatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ArtisanalDarkColorScheme else ArtisanalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = ArtisanalShapes,
        content = content
    )
}
