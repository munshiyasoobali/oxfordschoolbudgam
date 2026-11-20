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

private val OxfordLightColorScheme = lightColorScheme(
    primary = OxfordNavy,
    onPrimary = CrispWhite,
    primaryContainer = OxfordNavyContainer,
    onPrimaryContainer = OxfordNavy,
    secondary = HeritageGold,
    onSecondary = InkDark,
    secondaryContainer = HeritageGoldContainer,
    onSecondaryContainer = HeritageGoldOnContainer,
    tertiary = OxfordNavyLight,
    onTertiary = CrispWhite,
    background = AcademicOffWhite,
    onBackground = InkDark,
    surface = CrispWhite,
    onSurface = InkDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline,
    error = StatusAbsentRed,
    onError = CrispWhite,
    errorContainer = StatusAbsentBg,
    onErrorContainer = StatusAbsentRed
)

private val OxfordDarkColorScheme = darkColorScheme(
    primary = HeritageGoldLight,
    onPrimary = InkDark,
    primaryContainer = OxfordNavyLight,
    onPrimaryContainer = CrispWhite,
    secondary = HeritageGold,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFF423204),
    onSecondaryContainer = HeritageGoldLight,
    tertiary = OxfordNavyContainer,
    onTertiary = OxfordNavy,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = InkDark,
    errorContainer = Color(0xFF5F1515),
    onErrorContainer = Color(0xFFFECACA)
)

val OxfordShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun OxfordSchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OxfordDarkColorScheme else OxfordLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OxfordTypography,
        shapes = OxfordShapes,
        content = content
    )
}
