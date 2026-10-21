package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive Design System Color Palettes & Customization Presets
 * for "مدير يومي" (Daily Manager).
 */

enum class ThemePreset(
    val id: String,
    val arabicName: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryContainerLight: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerLight: Color,
    val onPrimaryContainerDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val previewColor: Color
) {
    EMERALD(
        id = "emerald",
        arabicName = "الزمرد العربي",
        primaryLight = Color(0xFF105844),
        primaryDark = Color(0xFF38D5A4),
        primaryContainerLight = Color(0xFFD3F5E9),
        primaryContainerDark = Color(0xFF0F4D3C),
        onPrimaryContainerLight = Color(0xFF053E30),
        onPrimaryContainerDark = Color(0xFF75F8CD),
        secondaryLight = Color(0xFFD4972C),
        secondaryDark = Color(0xFFE5B558),
        previewColor = Color(0xFF105844)
    ),
    SAPPHIRE(
        id = "sapphire",
        arabicName = "الياقوت الأزرق",
        primaryLight = Color(0xFF1E5B94),
        primaryDark = Color(0xFF76BAFC),
        primaryContainerLight = Color(0xFFD2E6FF),
        primaryContainerDark = Color(0xFF00497D),
        onPrimaryContainerLight = Color(0xFF003056),
        onPrimaryContainerDark = Color(0xFFCCE5FF),
        secondaryLight = Color(0xFF007A87),
        secondaryDark = Color(0xFF55D8E8),
        previewColor = Color(0xFF1E5B94)
    ),
    AMETHYST(
        id = "amethyst",
        arabicName = "البنفسج الملكي",
        primaryLight = Color(0xFF6B45A3),
        primaryDark = Color(0xFFD2B0FF),
        primaryContainerLight = Color(0xFFEEDBFF),
        primaryContainerDark = Color(0xFF512A88),
        onPrimaryContainerLight = Color(0xFF280058),
        onPrimaryContainerDark = Color(0xFFF3E8FF),
        secondaryLight = Color(0xFFBF507A),
        secondaryDark = Color(0xFFFFAFD1),
        previewColor = Color(0xFF6B45A3)
    ),
    TERRACOTTA(
        id = "terracotta",
        arabicName = "الغروب الدافئ",
        primaryLight = Color(0xFFAC4B28),
        primaryDark = Color(0xFFFFB59C),
        primaryContainerLight = Color(0xFFFFDBD0),
        primaryContainerDark = Color(0xFF8A3211),
        onPrimaryContainerLight = Color(0xFF3B0E00),
        onPrimaryContainerDark = Color(0xFFFFDBCF),
        secondaryLight = Color(0xFFB57C1E),
        secondaryDark = Color(0xFFF0BD68),
        previewColor = Color(0xFFAC4B28)
    );

    companion object {
        fun fromId(id: String?): ThemePreset = entries.find { it.id == id } ?: EMERALD
    }
}

// ==========================================
// 1. Primary Spectrum (Default Emerald / Jade)
// ==========================================
val EmeraldDeep = Color(0xFF093126)
val EmeraldPrimary = Color(0xFF105844)
val EmeraldLight = Color(0xFF1D7E63)
val MintAccent = Color(0xFF38D5A4)
val MintContainerLight = Color(0xFFD3F5E9)
val MintOnContainerLight = Color(0xFF053E30)
val MintContainerDark = Color(0xFF0F4D3C)
val MintOnContainerDark = Color(0xFF75F8CD)

// ==========================================
// 2. Secondary Spectrum (Warm Amber / Sand / Ochre)
// ==========================================
val GoldWarm = Color(0xFFD4972C)
val GoldSoft = Color(0xFFE5B558)
val GoldContainerLight = Color(0xFFFEF0D4)
val GoldOnContainerLight = Color(0xFF4C3305)
val GoldContainerDark = Color(0xFF412B08)
val GoldOnContainerDark = Color(0xFFFFDFA6)

// ==========================================
// 3. Tertiary Spectrum (Expressive Iris / Violet / AI Glow)
// ==========================================
val IrisTertiary = Color(0xFF6B57D2)
val IrisTertiaryLight = Color(0xFF8270E4)
val IrisContainerLight = Color(0xFFECE7FF)
val IrisOnContainerLight = Color(0xFF271578)
val IrisContainerDark = Color(0xFF352485)
val IrisOnContainerDark = Color(0xFFD6CFFF)

// ==========================================
// 4. M3 Expressive Surfaces & Tonal Containers (Light)
// ==========================================
val SandBackgroundLight = Color(0xFFF9F7F2)
val SandSurfaceLight = Color(0xFFFFFFFF)
val SandSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SandSurfaceContainerLowLight = Color(0xFFF5F2EA)
val SandSurfaceContainerLight = Color(0xFFEFECE3)
val SandSurfaceContainerHighLight = Color(0xFFE9E5DC)
val SandSurfaceContainerHighestLight = Color(0xFFE3DFD5)
val SandSurfaceVariantLight = Color(0xFFE4DFD6)
val SandOutlineLight = Color(0xFF7B7973)
val SandOutlineVariantLight = Color(0xFFCCC7BD)

val TextPrimaryLight = Color(0xFF191D1A)
val TextSecondaryLight = Color(0xFF434A45)
val TextTertiaryLight = Color(0xFF707872)

// ==========================================
// 5. M3 Expressive Surfaces & Tonal Containers (Dark)
// ==========================================
val NightBackgroundDark = Color(0xFF000000)
val NightSurfaceDark = Color(0xFF101010)
val NightSurfaceContainerLowestDark = Color(0xFF080808)
val NightSurfaceContainerLowDark = Color(0xFF121212)
val NightSurfaceContainerDark = Color(0xFF161616)
val NightSurfaceContainerHighDark = Color(0xFF1C1C1C)
val NightSurfaceContainerHighestDark = Color(0xFF242424)
val NightSurfaceVariantDark = Color(0xFF1A1A1A)
val NightOutlineDark = Color(0xFF282828)
val NightOutlineVariantDark = Color(0xFF1F1F1F)

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF8E8E93)
val TextTertiaryDark = Color(0xFF5A5A60)

// ==========================================
// 6. Semantic Alerts & Expressive Status
// ==========================================
val PriorityHighRed = Color(0xFFDE3737)
val PriorityHighContainerLight = Color(0xFFFFDAD6)
val PriorityHighOnContainerLight = Color(0xFF410002)

val PriorityMediumAmber = Color(0xFFD98218)
val PriorityMediumContainerLight = Color(0xFFFFDCC1)
val PriorityMediumOnContainerLight = Color(0xFF381A00)

val PriorityLowGreen = Color(0xFF218A58)
val PriorityLowContainerLight = Color(0xFFCEF6DF)
val PriorityLowOnContainerLight = Color(0xFF003820)

// Focus / Timer vibrant states
val FocusActiveCyan = Color(0xFF02A3A3)
val PrayerGoldenAccent = Color(0xFFF2BA49)
