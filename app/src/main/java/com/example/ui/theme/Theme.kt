package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BummmColorScheme = darkColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Blue,
    onSecondary = Color.White,
    tertiary = Green,
    onTertiary = Color.White,
    background = OuterBg,
    onBackground = Color.White,
    surface = Navy,
    onSurface = Color.White,
    error = Red,
    onError = Color.White
)

@Composable
fun BummmTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BummmColorScheme,
        typography = Typography,
        content = content
    )
}
