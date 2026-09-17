package fr.cartulaire.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.cartulaire.R

val CinzelFamily = FontFamily(
    Font(R.font.cinzel_decorative_bold, FontWeight.Bold),
)

val CormorantFamily = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
)

val BlackletterFamily = FontFamily(
    Font(R.font.unifraktur_cook, FontWeight.Bold),
)

val CartulaireTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 2.sp,
        color = Ink,
    ),
    headlineMedium = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.2.sp,
        color = Ink,
    ),
    titleLarge = TextStyle(
        fontFamily = CinzelFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
        color = Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = CormorantFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        color = Ink,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = CormorantFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = InkSoft,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = CormorantFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Ink,
    ),
)
