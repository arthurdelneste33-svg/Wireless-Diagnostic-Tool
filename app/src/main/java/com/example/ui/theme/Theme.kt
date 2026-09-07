package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppColorPalette(
    val title: String,
    val description: String,
    val previewColor: Color,
    val accentColor: Color
) {
    CYBER_CYAN(
        title = "Cyber Cyan (Défaut)",
        description = "Teinte néon futuriste et scanner haute fréquence",
        previewColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF26E6B8)
    ),
    MATRIX_EMERALD(
        title = "Matrix Emerald",
        description = "Vert hacker émeraude et terminal système",
        previewColor = Color(0xFF00E676),
        accentColor = Color(0xFF1DE9B6)
    ),
    SYNTHWAVE_VIOLET(
        title = "Synthwave Violet",
        description = "Pourpre néon rétro-futuriste et magenta vibrant",
        previewColor = Color(0xFFD500F9),
        accentColor = Color(0xFFB388FF)
    ),
    ELECTRIC_AMBER(
        title = "Electric Amber",
        description = "Or ionisé, radiation ambrée et haute énergie",
        previewColor = Color(0xFFFFAB00),
        accentColor = Color(0xFFFFD600)
    ),
    CRIMSON_ALERT(
        title = "Crimson Alert",
        description = "Rouge balistique et alerte diagnostic critique",
        previewColor = Color(0xFFFF1744),
        accentColor = Color(0xFFFF5252)
    ),
    SAPPHIRE_OCEAN(
        title = "Sapphire Cobalt",
        description = "Bleu saphir profond et réseau cryptographique",
        previewColor = Color(0xFF2979FF),
        accentColor = Color(0xFF00B0FF)
    )
}

fun buildColorScheme(
    palette: AppColorPalette,
    darkTheme: Boolean,
    isAmoled: Boolean = false
): ColorScheme {
    val bgDark = if (isAmoled) Color(0xFF000000) else DarkBackground
    val surfaceDark = if (isAmoled) Color(0xFF070B10) else DarkSurface
    val surfaceVarDark = if (isAmoled) Color(0xFF0F151F) else DarkSurfaceVariant

    return when (palette) {
        AppColorPalette.CYBER_CYAN -> if (darkTheme) {
            darkColorScheme(
                primary = CyanPrimaryDark,
                onPrimary = CyanOnPrimaryDark,
                primaryContainer = CyanPrimaryContainerDark,
                onPrimaryContainer = CyanOnPrimaryContainerDark,
                secondary = TealSecondaryDark,
                onSecondary = TealOnSecondaryDark,
                secondaryContainer = TealSecondaryContainerDark,
                onSecondaryContainer = TealOnSecondaryContainerDark,
                tertiary = IndigoTertiaryDark,
                onTertiary = IndigoOnTertiaryDark,
                background = bgDark,
                onBackground = Color(0xFFE2E8F0),
                surface = surfaceDark,
                onSurface = Color(0xFFE2E8F0),
                surfaceVariant = surfaceVarDark,
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = DarkOutline
            )
        } else {
            lightColorScheme(
                primary = CyanPrimaryLight,
                onPrimary = CyanOnPrimaryLight,
                primaryContainer = CyanPrimaryContainerLight,
                onPrimaryContainer = CyanOnPrimaryContainerLight,
                secondary = TealSecondaryLight,
                onSecondary = TealOnSecondaryLight,
                tertiary = IndigoTertiaryLight,
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant,
                outline = LightOutline
            )
        }

        AppColorPalette.MATRIX_EMERALD -> if (darkTheme) {
            darkColorScheme(
                primary = EmeraldPrimaryDark,
                onPrimary = EmeraldOnPrimaryDark,
                primaryContainer = EmeraldPrimaryContainerDark,
                onPrimaryContainer = EmeraldOnPrimaryContainerDark,
                secondary = EmeraldSecondaryDark,
                onSecondary = Color(0xFF00382B),
                tertiary = EmeraldTertiaryDark,
                background = bgDark,
                onBackground = Color(0xFFE2E8F0),
                surface = surfaceDark,
                onSurface = Color(0xFFE2E8F0),
                surfaceVariant = surfaceVarDark,
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF1E3A28)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF008945),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF94FFB8),
                secondary = Color(0xFF00796B),
                tertiary = Color(0xFF558B2F),
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant
            )
        }

        AppColorPalette.SYNTHWAVE_VIOLET -> if (darkTheme) {
            darkColorScheme(
                primary = VioletPrimaryDark,
                onPrimary = VioletOnPrimaryDark,
                primaryContainer = VioletPrimaryContainerDark,
                onPrimaryContainer = VioletOnPrimaryContainerDark,
                secondary = VioletSecondaryDark,
                onSecondary = Color(0xFF33008F),
                tertiary = VioletTertiaryDark,
                background = bgDark,
                onBackground = Color(0xFFF3E5F5),
                surface = surfaceDark,
                onSurface = Color(0xFFF3E5F5),
                surfaceVariant = surfaceVarDark,
                onSurfaceVariant = Color(0xFFCE93D8),
                outline = Color(0xFF4A148C)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF9C27B0),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF3E5F5),
                secondary = Color(0xFF7B1FA2),
                tertiary = Color(0xFFC2185B),
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant
            )
        }

        AppColorPalette.ELECTRIC_AMBER -> if (darkTheme) {
            darkColorScheme(
                primary = AmberPrimaryDark,
                onPrimary = AmberOnPrimaryDark,
                primaryContainer = AmberPrimaryContainerDark,
                onPrimaryContainer = AmberOnPrimaryContainerDark,
                secondary = AmberSecondaryDark,
                onSecondary = Color(0xFF3E2723),
                tertiary = AmberTertiaryDark,
                background = bgDark,
                onBackground = Color(0xFFFFF8E1),
                surface = surfaceDark,
                onSurface = Color(0xFFFFF8E1),
                surfaceVariant = surfaceVarDark,
                onSurfaceVariant = Color(0xFFFFE082),
                outline = Color(0xFF4E342E)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFE65100),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE0B2),
                secondary = Color(0xFFF57F17),
                tertiary = Color(0xFFFFB300),
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant
            )
        }

        AppColorPalette.CRIMSON_ALERT -> if (darkTheme) {
            darkColorScheme(
                primary = CrimsonPrimaryDark,
                onPrimary = CrimsonOnPrimaryDark,
                primaryContainer = CrimsonPrimaryContainerDark,
                onPrimaryContainer = CrimsonOnPrimaryContainerDark,
                secondary = CrimsonSecondaryDark,
                onSecondary = Color(0xFF370008),
                tertiary = CrimsonTertiaryDark,
                background = bgDark,
                onBackground = Color(0xFFFFEBEE),
                surface = surfaceDark,
                onSurface = Color(0xFFFFEBEE),
                surfaceVariant = surfaceVarDark,
                onSurfaceVariant = Color(0xFFFFCDD2),
                outline = Color(0xFF4C1014)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFD50000),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFCDD2),
                secondary = Color(0xFFC2185B),
                tertiary = Color(0xFFFF5252),
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant
            )
        }

        AppColorPalette.SAPPHIRE_OCEAN -> if (darkTheme) {
            darkColorScheme(
                primary = SapphirePrimaryDark,
                onPrimary = SapphireOnPrimaryDark,
                primaryContainer = SapphirePrimaryContainerDark,
                onPrimaryContainer = SapphireOnPrimaryContainerDark,
                secondary = SapphireSecondaryDark,
                onSecondary = Color(0xFF002171),
                tertiary = SapphireTertiaryDark,
                background = bgDark,
                onBackground = Color(0xFFE3F2FD),
                surface = surfaceDark,
                onSurface = Color(0xFFE3F2FD),
                surfaceVariant = surfaceVarDark,
                onSurfaceVariant = Color(0xFF90CAF9),
                outline = Color(0xFF0D47A1)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF1565C0),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFBBDEFB),
                secondary = Color(0xFF0277BD),
                tertiary = Color(0xFF00ACC1),
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    palette: AppColorPalette = AppColorPalette.CYBER_CYAN,
    darkTheme: Boolean = true,
    isAmoled: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildColorScheme(palette, darkTheme, isAmoled)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

