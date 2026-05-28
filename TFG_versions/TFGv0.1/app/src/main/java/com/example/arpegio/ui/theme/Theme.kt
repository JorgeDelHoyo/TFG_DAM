package com.example.arpegio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ══════════════════════════════════════════════════════════
// 🎸 ARPEG.IO — Tema Material 3 (Lila / Violeta)
//    Colores fijos de marca. No se usan colores dinámicos
//    para mantener coherencia visual con el logo.
// ══════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary             = ArpegPrimary,           // Lavanda del logo
    onPrimary           = ArpegOnPrimary,          // Blanco
    primaryContainer    = ArpegPrimaryContainer,   // Violeta profundo
    onPrimaryContainer  = ArpegOnPrimaryContainer, // Lila claro

    secondary           = ArpegSecondary,
    onSecondary         = ArpegOnPrimary,
    secondaryContainer  = ArpegSecondaryContainer,
    onSecondaryContainer= ArpegOnSecondaryContainer,

    tertiary            = ArpegTertiary,
    onTertiary          = ArpegOnPrimary,
    tertiaryContainer   = ArpegTertiaryContainer,
    onTertiaryContainer = ArpegOnTertiaryContainer,

    background          = ArpegDarkBackground,
    onBackground        = ArpegOnDarkSurface,
    surface             = ArpegDarkSurface,
    onSurface           = ArpegOnDarkSurface,      // ← Texto blanco/lila claro
    surfaceVariant      = ArpegDarkSurfaceHigh,
    onSurfaceVariant    = ArpegOnDarkSurface,      // ← Texto visible en barras

    error               = ArpegError,
    onError             = ArpegOnError,

    outline             = ArpegOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary             = ArpegPrimaryDark,        // Lila medio (mejor contraste)
    onPrimary           = ArpegOnPrimary,
    primaryContainer    = ArpegOnPrimaryContainer,
    onPrimaryContainer  = ArpegPrimaryContainer,

    secondary           = ArpegSecondary,
    onSecondary         = ArpegOnPrimary,
    secondaryContainer  = ArpegOnSecondaryContainer,
    onSecondaryContainer= ArpegSecondaryContainer,

    tertiary            = ArpegTertiary,
    onTertiary          = ArpegOnPrimary,
    tertiaryContainer   = ArpegOnTertiaryContainer,
    onTertiaryContainer = ArpegTertiaryContainer,

    background          = ArpegLightBackground,
    onBackground        = ArpegOnLightSurface,
    surface             = ArpegLightSurface,
    onSurface           = ArpegOnLightSurface,
    surfaceVariant      = ArpegLightBackground,
    onSurfaceVariant    = ArpegOnLightSurface,

    error               = ArpegError,
    onError             = ArpegOnError,

    outline             = ArpegOutline
)

/**
 * 🎸 ArpegioTheme — Tema personalizado para Arpeg.io
 *
 * Usa colores fijos lila/violeta basados en el logo de la app.
 * Los colores dinámicos de Material You están DESACTIVADOS
 * para mantener la identidad visual de la marca.
 *
 * @param darkTheme Si es true, usa el tema oscuro
 * @param content El contenido de la UI
 */
@Composable
fun ArpegioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivamos colores dinámicos para forzar siempre la paleta lila de la marca
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barra de estado con el fondo oscuro violeta de la app
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}