package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Extended Theme Tokens.
 * Provides rich expressive corner radii, container elevations, and layout metrics.
 */
data class ExpressiveShapes(
    val extraSmall: CornerBasedShape = RoundedCornerShape(8.dp),
    val small: CornerBasedShape = RoundedCornerShape(12.dp),
    val medium: CornerBasedShape = RoundedCornerShape(18.dp),
    val large: CornerBasedShape = RoundedCornerShape(26.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(34.dp),
    val pill: CornerBasedShape = RoundedCornerShape(100.dp),
    val asymmetricCard: CornerBasedShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 28.dp
    )
)

data class ExpressiveSpacings(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val xxl: Dp = 36.dp
)

val LocalExpressiveShapes = staticCompositionLocalOf { ExpressiveShapes() }
val LocalExpressiveSpacings = staticCompositionLocalOf { ExpressiveSpacings() }

fun getExpressiveDarkColorScheme(preset: ThemePreset): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = preset.primaryDark,
        onPrimary = EmeraldDeep,
        primaryContainer = preset.primaryContainerDark,
        onPrimaryContainer = preset.onPrimaryContainerDark,
        secondary = preset.secondaryDark,
        onSecondary = EmeraldDeep,
        secondaryContainer = GoldContainerDark,
        onSecondaryContainer = GoldOnContainerDark,
        tertiary = IrisTertiaryLight,
        onTertiary = Color.White,
        tertiaryContainer = IrisContainerDark,
        onTertiaryContainer = IrisOnContainerDark,
        background = NightBackgroundDark,
        onBackground = TextPrimaryDark,
        surface = NightSurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = NightSurfaceVariantDark,
        onSurfaceVariant = TextSecondaryDark,
        surfaceContainerLowest = NightSurfaceContainerLowestDark,
        surfaceContainerLow = NightSurfaceContainerLowDark,
        surfaceContainer = NightSurfaceContainerDark,
        surfaceContainerHigh = NightSurfaceContainerHighDark,
        surfaceContainerHighest = NightSurfaceContainerHighestDark,
        outline = NightOutlineDark,
        outlineVariant = NightOutlineVariantDark,
        error = PriorityHighRed,
        onError = Color.White,
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )
}

fun getExpressiveLightColorScheme(preset: ThemePreset): androidx.compose.material3.ColorScheme {
    return lightColorScheme(
        primary = preset.primaryLight,
        onPrimary = Color.White,
        primaryContainer = preset.primaryContainerLight,
        onPrimaryContainer = preset.onPrimaryContainerLight,
        secondary = preset.secondaryLight,
        onSecondary = Color.White,
        secondaryContainer = GoldContainerLight,
        onSecondaryContainer = GoldOnContainerLight,
        tertiary = IrisTertiary,
        onTertiary = Color.White,
        tertiaryContainer = IrisContainerLight,
        onTertiaryContainer = IrisOnContainerLight,
        background = SandBackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SandSurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SandSurfaceVariantLight,
        onSurfaceVariant = TextSecondaryLight,
        surfaceContainerLowest = SandSurfaceContainerLowestLight,
        surfaceContainerLow = SandSurfaceContainerLowLight,
        surfaceContainer = SandSurfaceContainerLight,
        surfaceContainerHigh = SandSurfaceContainerHighLight,
        surfaceContainerHighest = SandSurfaceContainerHighestLight,
        outline = SandOutlineLight,
        outlineVariant = SandOutlineVariantLight,
        error = PriorityHighRed,
        onError = Color.White,
        errorContainer = PriorityHighContainerLight,
        onErrorContainer = PriorityHighOnContainerLight
    )
}

val M3ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun DailyManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    preset: ThemePreset = ThemePreset.EMERALD,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        getExpressiveDarkColorScheme(preset)
    } else {
        getExpressiveLightColorScheme(preset)
    }
    val shapes = ExpressiveShapes()
    val spacings = ExpressiveSpacings()

    // فرض اتجاه الواجهة من اليمين إلى اليسار (RTL) بالكامل مع دعم M3 Expressive
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalExpressiveShapes provides shapes,
        LocalExpressiveSpacings provides spacings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = M3ExpressiveShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DailyManagerTheme(darkTheme = darkTheme, preset = ThemePreset.EMERALD, content = content)
}
