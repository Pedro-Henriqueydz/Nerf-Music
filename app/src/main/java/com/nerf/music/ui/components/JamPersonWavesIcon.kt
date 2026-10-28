package com.nerf.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ícone customizado da NERF Jam:
 * Silhueta de uma pessoa em primeiro plano com 3 ondas acústicas curvas atrás
 * no estilo das ondas curvas do Spotify.
 */
@Composable
fun JamPersonWavesIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 1. Três ondas curvas estilo Spotify atrás/ao redor da pessoa (inclinadas suavemente)
        val waveCenter = Offset(w * 0.34f, h * 0.50f)
        val stroke1 = w * 0.075f
        val stroke2 = w * 0.072f
        val stroke3 = w * 0.068f

        // Onda interna
        val r1 = w * 0.27f
        drawArc(
            color = tint.copy(alpha = 0.95f),
            startAngle = -52f,
            sweepAngle = 96f,
            useCenter = false,
            topLeft = Offset(waveCenter.x - r1, waveCenter.y - r1),
            size = Size(r1 * 2f, r1 * 2f),
            style = Stroke(width = stroke1, cap = StrokeCap.Round)
        )

        // Onda intermediária
        val r2 = w * 0.43f
        drawArc(
            color = tint.copy(alpha = 0.88f),
            startAngle = -50f,
            sweepAngle = 92f,
            useCenter = false,
            topLeft = Offset(waveCenter.x - r2, waveCenter.y - r2),
            size = Size(r2 * 2f, r2 * 2f),
            style = Stroke(width = stroke2, cap = StrokeCap.Round)
        )

        // Onda externa (estilo Spotify)
        val r3 = w * 0.59f
        drawArc(
            color = tint.copy(alpha = 0.78f),
            startAngle = -48f,
            sweepAngle = 88f,
            useCenter = false,
            topLeft = Offset(waveCenter.x - r3, waveCenter.y - r3),
            size = Size(r3 * 2f, r3 * 2f),
            style = Stroke(width = stroke3, cap = StrokeCap.Round)
        )

        // 2. Silhueta da pessoa em primeiro plano (à esquerda/centro)
        val headRadius = w * 0.145f
        val headCenter = Offset(w * 0.30f, h * 0.36f)
        drawCircle(
            color = tint,
            radius = headRadius,
            center = headCenter
        )

        // Ombros / Corpo arredondado
        val bodyPath = Path().apply {
            val leftX = w * 0.07f
            val rightX = w * 0.53f
            val bottomY = h * 0.86f
            val shoulderTopY = h * 0.56f

            moveTo(leftX, bottomY)
            cubicTo(
                leftX, shoulderTopY + h * 0.04f,
                headCenter.x - headRadius * 1.15f, shoulderTopY,
                headCenter.x, shoulderTopY
            )
            cubicTo(
                headCenter.x + headRadius * 1.15f, shoulderTopY,
                rightX, shoulderTopY + h * 0.04f,
                rightX, bottomY
            )
            close()
        }
        drawPath(
            path = bodyPath,
            color = tint
        )
    }
}
