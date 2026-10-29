package com.nerf.music.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nerf.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utilitário responsável pela renderização em Bitmap e compartilhamento
 * de Stories/Cards Sociais e Trechos de Letras.
 * 
 * Design:
 * - Card único em proporção 9:16 preenchido com a cor pura/gradiente da capa (SEM borda cinza ou "imagem dentro de imagem").
 * - Capa do álbum com cantos arredondados, título em destaque, artista e branding.
 * - No Card de Letras: aspas ("") no texto selecionado, miniatura e metadados.
 */
object SocialShareHelper {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1920

    private data class PaletteColors(val topColor: Int, val bottomColor: Int, val cardColor: Int)

    suspend fun renderMusicShareCard(context: Context, track: Track): Bitmap = withContext(Dispatchers.IO) {
        val artworkBitmap = loadArtworkBitmap(context, track)
        val colors = extractPaletteColors(artworkBitmap)

        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fundo com Gradiente Dinâmico baseado na Capa
        val bgGradient = android.graphics.LinearGradient(
            0f, 0f, 0f, CARD_HEIGHT.toFloat(),
            colors.topColor, colors.bottomColor,
            android.graphics.Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = bgGradient }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // 2. Card Flutuante Semi-Opaco (Frosted Glass) perfeitamente enquadrado para WhatsApp Preview
        val cardLeft = 70f
        val cardRight = CARD_WIDTH - 70f
        val cardTop = 300f
        val cardBottom = 1580f
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Sombra suave sob o card
        val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44000000")
            setShadowLayer(40f, 0f, 18f, Color.parseColor("#66000000"))
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardShadowPaint)

        // Preenchimento semi-opaco translúcido
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cardColor
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardFillPaint)

        // Borda sutil especular
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(0x33, 255, 255, 255)
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBorderPaint)

        // 3. Capa do Álbum Centralizada
        val coverSize = 680f
        val coverLeft = (CARD_WIDTH - coverSize) / 2f
        val coverTop = cardTop + 60f
        val coverRect = RectF(coverLeft, coverTop, coverLeft + coverSize, coverTop + coverSize)

        val coverShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33000000")
            setShadowLayer(30f, 0f, 14f, Color.parseColor("#55000000"))
        }
        canvas.drawRoundRect(coverRect, 32f, 32f, coverShadowPaint)

        if (artworkBitmap != null) {
            val roundedCover = getRoundedCornerBitmap(artworkBitmap, 32f, coverSize.toInt(), coverSize.toInt())
            canvas.drawBitmap(roundedCover, coverLeft, coverTop, Paint(Paint.ANTI_ALIAS_FLAG))
        } else {
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2C2C2E")
            }
            canvas.drawRoundRect(coverRect, 32f, 32f, placeholderPaint)
        }

        val coverStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(0x2B, 255, 255, 255)
        }
        canvas.drawRoundRect(coverRect, 32f, 32f, coverStroke)

        // 4. Metadados (Título e Artista)
        val textPadding = cardLeft + 50f
        val textWidth = (cardRight - textPadding - 50f).toInt()
        val textStartY = coverTop + coverSize + 46f

        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(
            track.title, 0, track.title.length, titlePaint, textWidth
        ).setMaxLines(2).setEllipsize(android.text.TextUtils.TruncateAt.END).build()

        canvas.save()
        canvas.translate(textPadding, textStartY)
        titleLayout.draw(canvas)
        canvas.restore()

        val artistPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#CCFFFFFF")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val artistStartY = textStartY + titleLayout.height + 14f
        val artistLayout = StaticLayout.Builder.obtain(
            track.artist, 0, track.artist.length, artistPaint, textWidth
        ).setMaxLines(1).setEllipsize(android.text.TextUtils.TruncateAt.END).build()

        canvas.save()
        canvas.translate(textPadding, artistStartY)
        artistLayout.draw(canvas)
        canvas.restore()

        // 5. Linha Divisória Sutil e Rodapé Enquadrado no Preview
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x22, 255, 255, 255)
            strokeWidth = 1.5f
        }
        val dividerY = cardBottom - 95f
        canvas.drawLine(textPadding, dividerY, cardRight - 50f, dividerY, dividerPaint)

        val footerY = cardBottom - 48f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6600")
        }
        canvas.drawCircle(textPadding + 14f, footerY - 10f, 9f, dotPaint)

        val brandPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("NERF MUSIC", textPadding + 36f, footerY, brandPaint)

        val hintPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#A6FFFFFF")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Toque para ouvir a música", cardRight - 50f, footerY, hintPaint)

        bitmap
    }

    suspend fun renderLyricsShareCard(
        context: Context,
        track: Track,
        selectedLines: List<String>
    ): Bitmap = withContext(Dispatchers.IO) {
        val artworkBitmap = loadArtworkBitmap(context, track)
        val colors = extractPaletteColors(artworkBitmap)

        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fundo com Gradiente Dinâmico
        val bgGradient = android.graphics.LinearGradient(
            0f, 0f, 0f, CARD_HEIGHT.toFloat(),
            colors.topColor, colors.bottomColor,
            android.graphics.Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = bgGradient }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // 2. Card Flutuante Semi-Opaco com Espaço Ampliado (Sem vazio no preview)
        val cardLeft = 70f
        val cardRight = CARD_WIDTH - 70f
        val cardTop = 300f
        val cardBottom = 1580f
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44000000")
            setShadowLayer(40f, 0f, 18f, Color.parseColor("#66000000"))
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardShadowPaint)

        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cardColor
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardFillPaint)

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(0x33, 255, 255, 255)
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBorderPaint)

        // 3. Cabeçalho Dentro do Card: Mini Capa + Título + Artista
        val headerLeft = cardLeft + 50f
        val headerTop = cardTop + 50f
        val miniSize = 140f
        val miniRect = RectF(headerLeft, headerTop, headerLeft + miniSize, headerTop + miniSize)

        if (artworkBitmap != null) {
            val roundedMini = getRoundedCornerBitmap(artworkBitmap, 24f, miniSize.toInt(), miniSize.toInt())
            canvas.drawBitmap(roundedMini, headerLeft, headerTop, Paint(Paint.ANTI_ALIAS_FLAG))
        } else {
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#333333")
            }
            canvas.drawRoundRect(miniRect, 24f, 24f, placeholderPaint)
        }

        val infoLeft = headerLeft + miniSize + 28f
        val infoWidth = (cardRight - 50f - infoLeft).toInt()

        val trackTitlePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(
            track.title, 0, track.title.length, trackTitlePaint, infoWidth
        ).setMaxLines(1).setEllipsize(android.text.TextUtils.TruncateAt.END).build()

        canvas.save()
        canvas.translate(infoLeft, headerTop + 16f)
        titleLayout.draw(canvas)
        canvas.restore()

        val trackArtistPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#CCFFFFFF")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val artistLayout = StaticLayout.Builder.obtain(
            track.artist, 0, track.artist.length, trackArtistPaint, infoWidth
        ).setMaxLines(1).setEllipsize(android.text.TextUtils.TruncateAt.END).build()

        canvas.save()
        canvas.translate(infoLeft, headerTop + 24f + titleLayout.height)
        artistLayout.draw(canvas)
        canvas.restore()

        // Linha sutil separando cabeçalho das letras
        val headerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x22, 255, 255, 255)
            strokeWidth = 1.5f
        }
        val headerDividerY = headerTop + miniSize + 36f
        canvas.drawLine(headerLeft, headerDividerY, cardRight - 50f, headerDividerY, headerDividerPaint)

        // 4. Linhas da Letra com Aspas ("...") e Tipografia Ampliada
        val rawLines = selectedLines.take(4)
        val contentWidth = (cardRight - 50f - headerLeft).toInt()

        val (lyricsTextSize, lyricsLineSpacing) = when (rawLines.size) {
            1 -> 90f to 32f
            2 -> 80f to 28f
            3 -> 70f to 24f
            else -> 62f to 20f
        }

        val lyricsTextPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = lyricsTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(16f, 0f, 8f, Color.parseColor("#55000000"))
        }

        var currentLyricY = headerDividerY + 44f

        for ((idx, rawLine) in rawLines.withIndex()) {
            val textWithQuote = when {
                rawLines.size == 1 -> "\"$rawLine\""
                idx == 0 -> "\"$rawLine"
                idx == rawLines.lastIndex -> "$rawLine\""
                else -> rawLine
            }

            val lineLayout = StaticLayout.Builder.obtain(
                textWithQuote, 0, textWithQuote.length, lyricsTextPaint, contentWidth
            ).setLineSpacing(lyricsLineSpacing, 1f)
             .setMaxLines(3)
             .setEllipsize(android.text.TextUtils.TruncateAt.END)
             .build()

            canvas.save()
            canvas.translate(headerLeft, currentLyricY)
            lineLayout.draw(canvas)
            canvas.restore()

            currentLyricY += lineLayout.height + 36f
        }

        // 5. Linha Divisória e Rodapé com Branding Oficial Enquadrado no Preview
        val footerDividerY = cardBottom - 95f
        canvas.drawLine(headerLeft, footerDividerY, cardRight - 50f, footerDividerY, headerDividerPaint)

        val footerY = cardBottom - 48f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6600")
        }
        canvas.drawCircle(headerLeft + 14f, footerY - 10f, 9f, dotPaint)

        val brandPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("NERF MUSIC • Letras", headerLeft + 36f, footerY, brandPaint)

        val hintPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#A6FFFFFF")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Toque para ouvir a música", cardRight - 50f, footerY, hintPaint)

        bitmap
    }

    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        title: String,
        deepLinkUrl: String
    ) = withContext(Dispatchers.IO) {
        val shareDir = File(context.cacheDir, "share_cards")
        if (!shareDir.exists()) shareDir.mkdirs()

        val shareFile = File(shareDir, "share_${System.currentTimeMillis()}.png")
        FileOutputStream(shareFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        withContext(Dispatchers.Main) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "🎵 $title\n$deepLinkUrl")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Compartilhar").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    private fun extractPaletteColors(bitmap: Bitmap?): PaletteColors {
        if (bitmap == null) {
            return PaletteColors(Color.parseColor("#261815"), Color.parseColor("#120B0A"), Color.argb(0x55, 255, 200, 170))
        }
        return try {
            val palette = Palette.from(bitmap).generate()
            val vibrant = palette.vibrantSwatch?.rgb
            val darkVibrant = palette.darkVibrantSwatch?.rgb
            val muted = palette.mutedSwatch?.rgb
            val darkMuted = palette.darkMutedSwatch?.rgb
            val dominant = palette.dominantSwatch?.rgb ?: Color.parseColor("#261815")

            // Cor realmente dominante da capa (maior população de pixels), sem acentos pequenos
            val chosen = palette.dominantSwatch?.rgb ?: vibrant ?: muted ?: dominant
            val hsv = FloatArray(3)
            Color.colorToHSV(chosen, hsv)
            val hue = hsv[0]
            val sat = hsv[1].coerceIn(0f, 0.85f)
            val topValue = hsv[2].coerceIn(0.38f, 0.68f)

            val top = Color.HSVToColor(floatArrayOf(hue, sat, topValue))
            val bottom = Color.HSVToColor(floatArrayOf(hue, sat, (topValue * 0.3f).coerceIn(0.10f, 0.2f)))

            // Cor do card translúcido: acento da própria capa (diferente do fundo), mais clara
            val accent = palette.lightVibrantSwatch?.rgb ?: palette.vibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: chosen
            val ahsv = FloatArray(3)
            Color.colorToHSV(accent, ahsv)
            val cardTint = Color.HSVToColor(
                0x66,
                floatArrayOf(ahsv[0], ahsv[1].coerceIn(0.25f, 0.8f), ahsv[2].coerceIn(0.75f, 1f))
            )

            PaletteColors(top, bottom, cardTint)
        } catch (_: Exception) {
            PaletteColors(Color.parseColor("#261815"), Color.parseColor("#120B0A"), Color.argb(0x55, 255, 200, 170))
        }
    }

    private suspend fun loadArtworkBitmap(context: Context, track: Track): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val offlineFile = File(context.filesDir, "offline_audio/${track.id}.jpg")
            val candidate = if (offlineFile.exists()) {
                "file://${offlineFile.absolutePath}"
            } else {
                track.highResThumbnailUrl ?: track.thumbnailUrl
            }
            if (candidate.isNullOrBlank()) return@withContext null

            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(candidate)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float, targetW: Int, targetH: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        val rect = Rect(0, 0, targetW, targetH)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, rect, rect, paint)

        return output
    }
}

