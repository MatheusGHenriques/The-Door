package io.github.matheusghenriques.thedoor.ui.theme

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

private val DarkCyanPrimary = Color(0xFF00838F)
private val DarkCyanOnPrimary = Color(0xFF111318)
private val DarkCyanSecondary = Color(0xFF4DD0E1)
private val DarkCyanBackground = Color(0xFF111318)
private val DarkCyanSurface = Color(0xFF1A1C23)
private val DarkCyanSurfaceVariant = Color(0xFF23252E)
private val DarkCyanOnBackground = Color(0xFFE0E0E0)
private val DarkCyanOnSurface = Color(0xFFE0E0E0)

private val LightCyanPrimary = Color(0xFF007582)
private val LightCyanOnPrimary = Color(0xFFFFFFFF)
private val LightCyanSecondary = Color(0xFF00838F)
private val LightCyanBackground = Color(0xFFF5F5F5)
private val LightCyanSurface = Color(0xFFFFFFFF)
private val LightCyanOnBackground = Color(0xFF111318)
private val LightCyanOnSurface = Color(0xFF111318)
private val LightCyanSurfaceVariant = Color(0xFFE8E8E8)

private val DarkCyanFallbackScheme = darkColorScheme(
    primary = DarkCyanPrimary,
    onPrimary = DarkCyanOnPrimary,
    secondary = DarkCyanSecondary,
    background = DarkCyanBackground,
    surface = DarkCyanSurface,
    surfaceVariant = DarkCyanSurfaceVariant,
    onBackground = DarkCyanOnBackground,
    onSurface = DarkCyanOnSurface,
)

private val LightCyanFallbackScheme = lightColorScheme(
    primary = LightCyanPrimary,
    onPrimary = LightCyanOnPrimary,
    secondary = LightCyanSecondary,
    background = LightCyanBackground,
    surface = LightCyanSurface,
    surfaceVariant = LightCyanSurfaceVariant,
    onBackground = LightCyanOnBackground,
    onSurface = LightCyanOnSurface,
)

@Composable
fun TheDoorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkCyanFallbackScheme
        else -> LightCyanFallbackScheme
    }

    MaterialTheme(
        colorScheme = colorScheme, content = content
    )
}
