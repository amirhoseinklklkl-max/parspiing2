package ir.amir.parsping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ParsPingGreen,
    secondary = ParsPingGreenLight,
    background = Color(0xFFF7F8F7),
)

private val DarkColors = darkColorScheme(
    primary = ParsPingGreenLight,
    secondary = ParsPingGreen,
    background = ParsPingDark,
    surface = ParsPingSurfaceDark,
)

@Composable
fun ParsPingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ParsPingTypography,
        content = content
    )
}
