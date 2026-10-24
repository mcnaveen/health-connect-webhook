package com.hcwebhook.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand scheme from the app icon pink (#E81E62): neutral warm surfaces, the vivid pink only
// for primary actions and the status card. Tertiary is green, for success states.
private val BrandLight = lightColorScheme(
    primary = Color(0xFFB80049),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2175E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF75565B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF5C3F43),
    tertiary = Color(0xFF006A35),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB7F1C4),
    onTertiaryContainer = Color(0xFF00210D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF22191A),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF22191A),
    surfaceVariant = Color(0xFFF3DDDF),
    onSurfaceVariant = Color(0xFF524345),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F1),
    surfaceContainer = Color(0xFFFBEAEB),
    surfaceContainerHigh = Color(0xFFF5E4E5),
    surfaceContainerHighest = Color(0xFFF0DEE0),
    outline = Color(0xFF847375),
    outlineVariant = Color(0xFFD6C2C3),
    inverseSurface = Color(0xFF382E2F),
    inverseOnSurface = Color(0xFFFEEDEE),
    inversePrimary = Color(0xFFFFB2BD),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFFFFB2BD),
    onPrimary = Color(0xFF670025),
    primaryContainer = Color(0xFFB80049),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFE5BDC1),
    onSecondary = Color(0xFF43292D),
    secondaryContainer = Color(0xFF5C3F43),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFF6DDC8E),
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005226),
    onTertiaryContainer = Color(0xFFB7F1C4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191112),
    onBackground = Color(0xFFF0DEE0),
    surface = Color(0xFF191112),
    onSurface = Color(0xFFF0DEE0),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C3),
    surfaceContainerLowest = Color(0xFF140C0D),
    surfaceContainerLow = Color(0xFF22191A),
    surfaceContainer = Color(0xFF261D1E),
    surfaceContainerHigh = Color(0xFF312829),
    surfaceContainerHighest = Color(0xFF3C3233),
    outline = Color(0xFF9F8C8E),
    outlineVariant = Color(0xFF524345),
    inverseSurface = Color(0xFFF0DEE0),
    inverseOnSurface = Color(0xFF382E2F),
    inversePrimary = Color(0xFFB80049),
)

@Composable
fun HCWebhookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Colors from the wallpaper (Android 12+). Off by default; the user turns it on in Settings.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> BrandDark
        else -> BrandLight
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
