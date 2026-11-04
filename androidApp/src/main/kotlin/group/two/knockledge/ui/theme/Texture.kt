package group.two.knockledge.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import group.two.knockledge.R

/** Texture opacity for the main application chassis canvas (26%). */
const val SpeakerClothAlphaBackground: Float = 0.26f

/** Texture opacity for the acoustic sensor bay header (34%). */
const val SpeakerClothAlphaSensorBay: Float = 0.34f

/**
 * Applies a seamless repeating acoustic speaker grill cloth texture over the composable surface.
 *
 * Employs [Modifier.drawWithCache] and [ShaderBrush] with [TileMode.Repeated] to guarantee
 * zero memory allocations during recomposition and zero main-thread layout overhead.
 * The texture draws before child content to preserve vector sharpness on typography and controls.
 *
 * @param alpha The blending opacity of the cloth pattern (default [SpeakerClothAlphaBackground]).
 * @param drawableRes The drawable resource representing the repeating acoustic tile.
 */
@Composable
fun Modifier.speakerClothTexture(
    alpha: Float = SpeakerClothAlphaBackground,
    @DrawableRes drawableRes: Int = R.drawable.texture_speaker_grill,
): Modifier {
    val image = ImageBitmap.imageResource(drawableRes)
    return drawWithCache {
        val brush = ShaderBrush(
            ImageShader(
                image = image,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            ),
        )
        onDrawBehind {
            drawRect(brush = brush, alpha = alpha)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeakerClothTexturePreview() {
    KnockledgeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianMidnight)
                .speakerClothTexture(),
        )
    }
}
