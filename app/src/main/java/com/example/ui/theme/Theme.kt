package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OkuzFlightDeckColorScheme = darkColorScheme(
    primary = CommodoreAmber,
    onPrimary = NostromoVoidBg,
    primaryContainer = CommodoreAmberDim,
    onPrimaryContainer = CommodoreAmberBright,
    secondary = NostromoTeal,
    onSecondary = NostromoVoidBg,
    secondaryContainer = NostromoTealDim,
    onSecondaryContainer = NostromoTealBright,
    tertiary = FireflyCopper,
    onTertiary = NostromoVoidBg,
    error = HazardRose,
    errorContainer = HazardRoseDim,
    background = NostromoVoidBg,
    onBackground = TerminalTextPrimary,
    surface = NostromoSlateSurface,
    onSurface = TerminalTextPrimary,
    surfaceVariant = NostromoPanelCard,
    onSurfaceVariant = TerminalTextSecondary,
    outline = BulkheadBorder,
    outlineVariant = BulkheadDivider
)

@Composable
fun ProjectOkuzTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OkuzFlightDeckColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    ProjectOkuzTheme(content = content)
}
