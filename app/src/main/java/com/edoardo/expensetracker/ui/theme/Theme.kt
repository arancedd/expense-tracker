package com.edoardo.expensetracker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMint,
    secondary = PrimaryMintLight,
    tertiary = AccentSky,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceElevated,
    surfaceContainerLow = SurfaceDark,
    surfaceContainerLowest = BackgroundDark,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ExpenseRed,
    surfaceTint = Color.Transparent,
    scrim = Color.Black
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = true, // Default Modern Ivy Deep Neutral Dark (Zero Blue/Violet Tint)
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
