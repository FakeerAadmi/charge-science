package com.example.chargescience.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Calibrated OLED dark theme for precision battery diagnostics
private val DiagnosticDarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D4EB),
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5C),
    onPrimaryContainer = Color(0xFFA6EEFF),
    secondary = Color(0xFFB1CBD0),
    onSecondary = Color(0xFF1B3438),
    secondaryContainer = Color(0xFF334B4F),
    onSecondaryContainer = Color(0xFFCDE7EC),
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF561E17),
    tertiaryContainer = Color(0xFF73332B),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF0E1416),
    onBackground = Color(0xFFDEE3E5),
    surface = Color(0xFF0E1416),
    onSurface = Color(0xFFDEE3E5),
    surfaceVariant = Color(0xFF3F484A),
    onSurfaceVariant = Color(0xFFBFC8CA),
    outline = Color(0xFF889295),
    outlineVariant = Color(0xFF3F484A),
    surfaceContainerLowest = Color(0xFF090F11),
    surfaceContainerLow = Color(0xFF161C1E),
    surfaceContainer = Color(0xFF1A2123),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF2F3638)
)

private val DiagnosticLightColorScheme = lightColorScheme(
    primary = Color(0xFF006877),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA6EEFF),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Color(0xFF4A6267),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = Color(0xFF904A41),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF3B0905),
    background = Color(0xFFF5FAFB),
    onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF5FAFB),
    onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDBE4E6),
    onSurfaceVariant = Color(0xFF3F484A),
    outline = Color(0xFF6F797A),
    outlineVariant = Color(0xFFBFC8CA)
)

@Composable
fun ChargeScienceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DiagnosticDarkColorScheme
        else -> DiagnosticLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
