package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class StudioExtendedColors(
    val hairline: Color,
    val masteryHigh: Color,
    val masteryHighContainer: Color,
    val masteryMid: Color,
    val masteryMidContainer: Color,
    val masteryLow: Color,
    val masteryLowContainer: Color,
    val vocalTractFill: Color,
    val vocalTractOutline: Color,
    val tongueFill: Color,
    val tongueStroke: Color
)

val LocalStudioColors = staticCompositionLocalOf {
    StudioExtendedColors(
        hairline = StudioHairline,
        masteryHigh = MasteryEmerald,
        masteryHighContainer = MasteryEmeraldContainer,
        masteryMid = StudioAmber,
        masteryMidContainer = StudioAmberContainer,
        masteryLow = VermilionCue,
        masteryLowContainer = VermilionContainer,
        vocalTractFill = StudioSand,
        vocalTractOutline = CarbonInk,
        tongueFill = VermilionCue,
        tongueStroke = AcousticTealDark
    )
}

private val VoxFormLightColorScheme = lightColorScheme(
    primary = AcousticTeal,
    onPrimary = Color.White,
    primaryContainer = AcousticTealContainer,
    onPrimaryContainer = OnAcousticTealContainer,
    secondary = VermilionCue,
    onSecondary = Color.White,
    secondaryContainer = VermilionContainer,
    onSecondaryContainer = OnVermilionContainer,
    tertiary = StudioAmber,
    onTertiary = Color.White,
    tertiaryContainer = StudioAmberContainer,
    onTertiaryContainer = OnStudioAmberContainer,
    background = StudioLinen,
    onBackground = CarbonInk,
    surface = StudioCeramic,
    onSurface = CarbonInk,
    surfaceVariant = StudioSand,
    onSurfaceVariant = GraphiteMuted,
    surfaceContainerLowest = StudioCeramic,
    surfaceContainerLow = Color(0xFFFAF8F3),
    surfaceContainer = StudioSand,
    surfaceContainerHigh = StudioSandDeep,
    outline = StudioHairline,
    outlineVariant = Color(0xFFE6E0D4),
    error = VermilionCue,
    onError = Color.White,
    errorContainer = VermilionContainer,
    onErrorContainer = OnVermilionContainer
)

private val VoxFormDarkColorScheme = darkColorScheme(
    primary = AcousticTealLight,
    onPrimary = Color(0xFF032A2A),
    primaryContainer = AcousticTealDark,
    onPrimaryContainer = AcousticTealContainer,
    secondary = VermilionLight,
    onSecondary = Color(0xFF470E04),
    secondaryContainer = Color(0xFF702212),
    onSecondaryContainer = VermilionContainer,
    tertiary = AmberLight,
    onTertiary = Color(0xFF3E2402),
    tertiaryContainer = Color(0xFF5D3A08),
    onTertiaryContainer = StudioAmberContainer,
    background = NightStudioBg,
    onBackground = NightInk,
    surface = NightStudioSurface,
    onSurface = NightInk,
    surfaceVariant = NightStudioContainer,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF161A1C),
    surfaceContainerLow = NightStudioSurface,
    surfaceContainer = NightStudioContainer,
    surfaceContainerHigh = Color(0xFF2D3538),
    outline = NightStudioHairline,
    outlineVariant = Color(0xFF283033)
)

private val VoxFormShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VoxFormTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Precision acoustic studio defaults to warm light surfaces per user preference
    val colorScheme = if (darkTheme) VoxFormDarkColorScheme else VoxFormLightColorScheme
    val extendedColors = if (darkTheme) {
        StudioExtendedColors(
            hairline = NightStudioHairline,
            masteryHigh = Color(0xFF57C695),
            masteryHighContainer = Color(0xFF123C2B),
            masteryMid = AmberLight,
            masteryMidContainer = Color(0xFF4A310A),
            masteryLow = VermilionLight,
            masteryLowContainer = Color(0xFF5A1D12),
            vocalTractFill = NightStudioContainer,
            vocalTractOutline = NightInk,
            tongueFill = VermilionLight,
            tongueStroke = AcousticTealLight
        )
    } else {
        StudioExtendedColors(
            hairline = StudioHairline,
            masteryHigh = MasteryEmerald,
            masteryHighContainer = MasteryEmeraldContainer,
            masteryMid = StudioAmber,
            masteryMidContainer = StudioAmberContainer,
            masteryLow = VermilionCue,
            masteryLowContainer = VermilionContainer,
            vocalTractFill = StudioSand,
            vocalTractOutline = CarbonInk,
            tongueFill = VermilionCue,
            tongueStroke = AcousticTealDark
        )
    }

    CompositionLocalProvider(LocalStudioColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VoxFormTypography,
            shapes = VoxFormShapes,
            content = content
        )
    }
}
