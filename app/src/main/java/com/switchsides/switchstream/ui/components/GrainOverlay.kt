package com.switchsides.switchstream.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * A static grain / film texture overlay. Renders once at composition time into a tiny
 * bitmap (seeded so it's deterministic across recompositions) and tiles it across the
 * surface. Gives dark UI surfaces a cinematic analog texture without shipping an asset.
 *
 * Meant to be stacked on top of images or dark backgrounds via `Modifier.then(filmGrain())`.
 * Keep alpha low (0.02–0.04) — grain should be felt, not seen.
 */
@Composable
fun Modifier.filmGrain(
    alpha: Float = 0.035f,
    tileSize: Dp = 96.dp,
    seed: Int = 7,
): Modifier = this.then(
    remember(alpha, tileSize, seed) {
        Modifier.drawWithCache {
            val tilePx = tileSize.toPx().toInt().coerceAtLeast(32)
            val rng = Random(seed)
            val tint = Color.White.copy(alpha = alpha).toArgb()
            val bitmap = android.graphics.Bitmap.createBitmap(
                tilePx, tilePx, android.graphics.Bitmap.Config.ARGB_8888
            )
            val pixels = IntArray(tilePx * tilePx)
            for (i in pixels.indices) {
                // Triangular noise distribution — feels more filmic than uniform.
                val v = (rng.nextFloat() + rng.nextFloat() - 1f)
                val a = (alpha * 255f * (0.5f + 0.5f * v)).toInt().coerceIn(0, 255)
                pixels[i] = (a shl 24) or (tint and 0x00FFFFFF)
            }
            bitmap.setPixels(pixels, 0, tilePx, 0, 0, tilePx, tilePx)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = false
                isFilterBitmap = false
                shader = android.graphics.BitmapShader(
                    bitmap,
                    android.graphics.Shader.TileMode.REPEAT,
                    android.graphics.Shader.TileMode.REPEAT
                )
            }
            onDrawWithContent {
                drawContent()
                drawIntoCanvas { c ->
                    c.nativeCanvas.drawRect(
                        0f, 0f, size.width, size.height, paint
                    )
                }
            }
        }
    }
)
