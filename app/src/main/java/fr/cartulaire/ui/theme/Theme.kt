package fr.cartulaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    secondary = Oxblood,
    onSecondary = Vellum,
    background = Parchment,
    onBackground = Ink,
    surface = Vellum,
    onSurface = Ink,
    tertiary = Verdigris,
)

@Composable
fun CartulaireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = CartulaireTypography,
        content = content,
    )
}
