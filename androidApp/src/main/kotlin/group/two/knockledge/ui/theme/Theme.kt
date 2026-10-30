package group.two.knockledge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KnockColorScheme = darkColorScheme(
    primary = KnockBrass,
    onPrimary = KnockInk,
    secondary = KnockGold,
    onSecondary = KnockInk,
    tertiary = KnockGlitchPink,
    onTertiary = KnockInk,
    background = KnockMidnight,
    onBackground = KnockCream,
    surface = KnockIndigo,
    onSurface = KnockCream,
    surfaceVariant = KnockIndigoHigh,
    onSurfaceVariant = KnockCreamDim,
    outline = KnockBrass,
    error = KnockError,
    onError = KnockInk,
)

@Composable
fun KnockledgeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KnockColorScheme,
        typography = Typography,
        content = content,
    )
}