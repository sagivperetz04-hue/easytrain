package com.easytrain.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SteelBlue,
    onPrimary = Paper,
    secondary = ChalkOrange,
    onSecondary = Paper,
    tertiary = PlateGreen,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceContainer = PaperContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = SteelBlueLight,
    onPrimary = SteelBlue,
    secondary = ChalkOrangeLight,
    onSecondary = Charcoal,
    tertiary = PlateGreenLight,
    onTertiary = Charcoal,
    background = Charcoal,
    onBackground = Chalk,
    surface = Charcoal,
    onSurface = Chalk,
    surfaceContainer = CharcoalContainer,
)

/**
 * Dynamic color is deliberately off: a coach's roster must look the same on every phone, and the
 * chalk-orange "action on the floor" colour carries meaning that a wallpaper-derived palette breaks.
 */
@Composable
fun EasyTrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        shapes = EasyTrainShapes,
        content = content,
    )
}
