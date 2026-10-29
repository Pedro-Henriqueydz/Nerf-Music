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
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nerf.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    enum class Style(val label: String) { GLASS("Vidro"), MINIMAL("Minimalista"), FULL_COVER("Capa inteira") }

    fun openPreview(context: Context, track: Track, lines: List<String> = emptyList()) {
        context.startActivity(Intent(context, com.nerf.music.ui.SharePreviewActivity::class.java).apply {
            putExtra("track", kotlinx.serialization.json.Json.encodeToString(Track.serializer(), track))
            putStringArrayListExtra("lines", ArrayList(ShareRules.cleanLines(lines)))
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    suspend fun renderCard(context: Context, track: Track, lines: List<String>, style: Style): Bitmap {
        val clean = ShareRules.cleanLines(lines)
        if (style == Style.GLASS) return if (clean.isEmpty()) renderMusicShareCard(context, track)
            else renderLyricsShareCard(context, track, clean)
        return withContext(Dispatchers.IO) {
            val artwork = loadArtworkBitmap(context, track)
            val colors = ArtworkColors.extract(artwork)
            val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(if (style == Style.MINIMAL) Color.rgb(18, 18, 20) else colors.top)
            if (artwork != null) {
                if (style == Style.FULL_COVER) {
                    val scale = maxOf(CARD_WIDTH.toFloat() / artwork.width, CARD_HEIGHT.toFloat() / artwork.height)
                    val width = artwork.width * scale
                    val height = artwork.height * scale
                    canvas.drawBitmap(artwork, null, RectF((CARD_WIDTH - width) / 2, (CARD_HEIGHT - height) / 2,
                        (CARD_WIDTH + width) / 2, (CARD_HEIGHT + height) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
                    canvas.drawRect(0f, 0f, 1080f, 1920f, Paint().apply {
                        shader = android.graphics.LinearGradient(0f, 0f, 0f, 1920f,
                            intArrayOf(0x55000000, 0x99000000.toInt(), 0xF2000000.toInt()),
                            null, android.graphics.Shader.TileMode.CLAMP)
                    })
                } else {
                    val cover = getRoundedCornerBitmap(artwork, 24f, 760, 760)
                    canvas.drawBitmap(cover, 160f, 170f, Paint(Paint.ANTI_ALIAS_FLAG))
                }
            }
            val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 72f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            fun drawText(text: String, y: Float, paint: TextPaint, maxLines: Int): Int {
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 820)
                    .setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
                canvas.save(); canvas.translate(130f, y); layout.draw(canvas); canvas.restore()
                return layout.height
            }
            val top = if (style == Style.MINIMAL) 1010f else if (clean.isEmpty()) 1260f else 360f
            var y = top + drawText(track.title, top, title, 2) + 20f
            val artist = TextPaint(title).apply { textSize = 40f; color = 0xCCFFFFFF.toInt(); typeface = Typeface.DEFAULT }
            y += drawText(track.artist, y, artist, 2) + 52f
            if (clean.isNotEmpty()) {
                val text = "\u201c" + clean.joinToString("\n") + "\u201d"
                val paint = TextPaint(title).apply { textSize = 68f }
                val available = (1710f - y).toInt().coerceAtLeast(100)
                var layout: StaticLayout
                do {
                    layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 820)
                        .setLineSpacing(12f, 1f).build()
                    if (layout.height <= available || paint.textSize <= 24f) break
                    paint.textSize -= 2f
                } while (true)
                canvas.save(); canvas.translate(130f, y)
                canvas.clipRect(0f, 0f, 820f, available.toFloat())
                layout.draw(canvas); canvas.restore()
            }
            drawText("NERF MUSIC", 1780f, TextPaint(title).apply { textSize = 30f }, 1)
            bitmap
        }
    }

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
        val rawLines = ShareRules.cleanLines(selectedLines)
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

        val lyricText = "\u201c" + rawLines.joinToString("\n") + "\u201d"
        val availableHeight = (cardBottom - 140f - (headerDividerY + 44f)).toInt()
        var fittedLayout: StaticLayout
        do {
            fittedLayout = StaticLayout.Builder.obtain(lyricText, 0, lyricText.length, lyricsTextPaint, contentWidth)
                .setLineSpacing(lyricsLineSpacing, 1f).build()
            if (fittedLayout.height <= availableHeight || lyricsTextPaint.textSize <= 24f) break
            lyricsTextPaint.textSize -= 2f
        } while (true)
        canvas.save()
        canvas.translate(headerLeft, headerDividerY + 44f)
        canvas.clipRect(0f, 0f, contentWidth.toFloat(), availableHeight.toFloat())
        fittedLayout.draw(canvas)
        canvas.restore()

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
        val uri = writeShareImage(context, bitmap)

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

    suspend fun writeShareImage(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "share_cards").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }
            ?.forEach { it.delete() }
        val file = File.createTempFile("card_", ".png", directory)
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    private fun extractPaletteColors(bitmap: Bitmap?): PaletteColors {
        val colors = ArtworkColors.extract(bitmap)
        return PaletteColors(colors.top, colors.bottom, colors.card)
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

            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(candidate)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float, targetW: Int, targetH: Int): Bitmap {
        val edge = minOf(bitmap.width, bitmap.height)
        val source = Rect((bitmap.width - edge) / 2, (bitmap.height - edge) / 2,
            (bitmap.width + edge) / 2, (bitmap.height + edge) / 2)
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
        canvas.drawBitmap(bitmap, source, rect, paint)

        return output
    }
}

