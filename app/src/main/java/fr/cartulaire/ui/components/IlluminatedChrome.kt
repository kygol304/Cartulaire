package fr.cartulaire.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.cartulaire.R
import fr.cartulaire.ui.theme.Gold
import fr.cartulaire.ui.theme.Ink
import fr.cartulaire.ui.theme.Oxblood
import fr.cartulaire.ui.theme.Parchment
import fr.cartulaire.ui.theme.Vellum

@Composable
fun ManuscriptCorners(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.illum_corner_tl),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(120.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(R.drawable.illum_corner_tr),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(120.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(R.drawable.illum_corner_bl),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(120.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(R.drawable.illum_corner_br),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(120.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun ParchmentPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, Gold, RoundedCornerShape(10.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.parchment_texture),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .background(Parchment.copy(alpha = 0.62f))
                .padding(12.dp),
            content = content,
        )
    }
}

@Composable
fun GoldRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Gold, Vellum, Gold, Color.Transparent),
                ),
            ),
    )
}
