package com.nerf.music.util

import android.content.Context
import android.content.ContentValues
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
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nerf.music.R
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.RecapSummary
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

    suspend fun renderRecapShareCard(context: Context, summary: RecapSummary): Bitmap = withContext(Dispatchers.IO) {
        val featured = summary.topTracks.firstOrNull()
        val artwork = featured?.track?.let { loadArtworkBitmap(context, it) }
        val colors = ArtworkColors.extract(artwork)
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
                intArrayOf(colors.top, colors.bottom, Color.rgb(8, 9, 11)),
                floatArrayOf(0f, 0.52f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), background)

        if (artwork != null) {
            val scale = maxOf(CARD_WIDTH.toFloat() / artwork.width, CARD_HEIGHT.toFloat() / artwork.height)
            val drawnWidth = artwork.width * scale
            val drawnHeight = artwork.height * scale
            canvas.drawBitmap(
                artwork,
                null,
                RectF(
                    (CARD_WIDTH - drawnWidth) / 2f,
                    (CARD_HEIGHT - drawnHeight) / 2f,
                    (CARD_WIDTH + drawnWidth) / 2f,
                    (CARD_HEIGHT + drawnHeight) / 2f
                ),
                Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 105 }
            )
            canvas.drawRect(
                0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
                Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, 0f, CARD_HEIGHT.toFloat(),
                        intArrayOf(0x9908090B.toInt(), 0xAA08090B.toInt(), 0xF508090B.toInt()),
                        floatArrayOf(0f, 0.48f, 1f),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
            )
        }

        val left = 92f
        val right = CARD_WIDTH - left
        val sfMedium = ResourcesCompat.getFont(context, R.font.sf_pro_medium) ?: Typeface.DEFAULT
        val sfSemibold = ResourcesCompat.getFont(context, R.font.sf_pro_semibold) ?: Typeface.DEFAULT_BOLD
        val sfBold = ResourcesCompat.getFont(context, R.font.sf_pro_bold) ?: Typeface.DEFAULT_BOLD

        fun textPaint(size: Float, color: Int, typeface: Typeface = sfMedium) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            this.typeface = typeface
        }
        fun drawText(text: String, x: Float, y: Float, width: Int, paint: TextPaint, maxLines: Int = 2): Int {
            if (text.isBlank()) return 0
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setMaxLines(maxLines)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
            return layout.height
        }

        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 215, 96) }
        canvas.drawCircle(left + 8f, 138f, 8f, accent)
        drawText("NERF MUSIC  /  RECAP ${summary.year}", left + 34f, 116f, 760, textPaint(28f, Color.WHITE, sfSemibold), 1)

        drawText("SEU ANO. SEU SOM.", left, 340f, 850, textPaint(35f, 0xCCFFFFFF.toInt(), sfSemibold), 1)
        drawText("${summary.totalMinutes / 60}h", left - 6f, 422f, 870, textPaint(164f, Color.WHITE, sfBold), 1)
        drawText("com música por perto", left + 6f, 620f, 820, textPaint(44f, 0xE6FFFFFF.toInt(), sfSemibold), 1)

        val panel = RectF(left, 780f, right, 1042f)
        canvas.drawRoundRect(panel, 42f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x24FFFFFF })
        canvas.drawRoundRect(panel, 42f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0x35FFFFFF
        })
        drawText("${summary.totalPlays} reproduções", left + 42f, 832f, 770, textPaint(46f, Color.WHITE, sfSemibold), 1)
        drawText("${summary.topArtist?.name ?: "Sua trilha sonora"} · artista mais ouvido", left + 42f, 916f, 770, textPaint(31f, 0xCCFFFFFF.toInt()), 1)

        if (featured != null) {
            val coverRect = RectF(left, 1110f, left + 220f, 1330f)
            if (artwork != null) {
                val cover = getRoundedCornerBitmap(artwork, 28f, 220, 220)
                canvas.drawBitmap(cover, coverRect.left, coverRect.top, Paint(Paint.ANTI_ALIAS_FLAG))
            } else {
                canvas.drawRoundRect(coverRect, 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.card })
            }
            drawText("FAIXA MAIS OUVIDA", left + 260f, 1138f, 600, textPaint(27f, 0xAAFFFFFF.toInt(), sfSemibold), 1)
            drawText(featured.track.title, left + 260f, 1192f, 600, textPaint(43f, Color.WHITE, sfBold), 2)
            drawText("${featured.track.artist} · ${featured.plays} plays", left + 260f, 1290f, 600, textPaint(29f, 0xC8FFFFFF.toInt()), 1)
        }

        drawText("AS 5 FAIXAS DO SEU ANO", left, 1432f, 800, textPaint(26f, 0xAAFFFFFF.toInt(), sfSemibold), 1)
        summary.topTracks.take(5).forEachIndexed { index, item ->
            val y = 1500f + index * 58f
            val line = "${index + 1}.  ${item.track.title}  ·  ${item.track.artist}"
            drawText(line, left, y, 890, textPaint(28f, if (index == 0) Color.WHITE else 0xCCFFFFFF.toInt(), if (index == 0) sfSemibold else sfMedium), 1)
        }
        drawText("NERF MUSIC", left, 1830f, 400, textPaint(29f, 0xBBFFFFFF.toInt(), sfBold), 1)
        bitmap
    }

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

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String, relativePath: String): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("NÃ£o foi possÃ­vel criar a imagem na galeria.")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Falha ao gravar a imagem." }
            } ?: error("NÃ£o foi possÃ­vel abrir o ficheiro da imagem.")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
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

