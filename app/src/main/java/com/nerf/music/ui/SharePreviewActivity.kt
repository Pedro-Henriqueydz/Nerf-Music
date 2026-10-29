package com.nerf.music.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.nerf.music.data.model.Track
import com.nerf.music.R
import com.nerf.music.ui.components.applePressFeedback
import com.nerf.music.ui.theme.NERFMusicTheme
import com.nerf.music.util.ShareRules
import com.nerf.music.util.SocialShareHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val ShareOrange = Color(0xFFFF7135)
private val GlassBorder = Color.White.copy(alpha = 0.14f)

class SharePreviewActivity : ComponentActivity() {
    private var exportBitmap: Bitmap? = null
    private val createImage = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bitmap = exportBitmap
        exportBitmap = null
        if (uri != null && bitmap != null) lifecycleScope.launch {
            perform { writeBitmap(uri, bitmap); message("Imagem salva") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val track = runCatching { Json.decodeFromString(Track.serializer(), intent.getStringExtra("track") ?: "") }.getOrNull()
        if (track == null) { finish(); return }
        val lines = ShareRules.cleanLines(intent.getStringArrayListExtra("lines").orEmpty())
        val link = "https://music.youtube.com/watch?v=${Uri.encode(track.id)}"
        setContent {
            NERFMusicTheme {
                var style by remember { mutableStateOf(SocialShareHelper.Style.GLASS) }
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var busy by remember { mutableStateOf(false) }
                var sheetVisible by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) { sheetVisible = true }
                LaunchedEffect(style) {
                    bitmap = null
                    error = null
                    try { bitmap = SocialShareHelper.renderCard(this@SharePreviewActivity, track, lines, style) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "Não foi possível gerar a prévia. Tente outro estilo." }
                }
                fun send(action: suspend (Bitmap) -> Unit) {
                    val image = bitmap ?: return
                    busy = true
                    scope.launch {
                        try { perform { action(image) } }
                        finally { busy = false }
                    }
                }
                SharePreviewSheet(
                    track, style, bitmap, error, busy, sheetVisible,
                    {
                        sheetVisible = false
                        scope.launch {
                            delay(180)
                            finish()
                        }
                    },
                    { style = it },
                    { send { shareTo(it, link, "com.instagram.android", stories = true) } },
                    { send { shareTo(it, link, "com.whatsapp", stories = false) } },
                    {
                        send { image ->
                            if (Build.VERSION.SDK_INT >= 29) {
                                saveToGallery(image)
                                message("Salvo em Pictures/NERF Music")
                            } else {
                                exportBitmap = image
                                createImage.launch("NERF_${System.currentTimeMillis()}.png")
                            }
                        }
                    },
                    { copyLink(link); message("Link copiado") }
                )
            }
        }
    }

    private suspend fun perform(action: suspend () -> Unit) {
        try { action() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: android.content.ActivityNotFoundException) { message("Aplicativo não instalado. Salve a imagem ou copie o link.") }
        catch (_: Exception) { message("Não foi possível concluir. Tente novamente.") }
    }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun copyLink(link: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Link da música", link))
    }

    private suspend fun shareTo(bitmap: Bitmap, link: String, target: String, stories: Boolean) {
        val uri = SocialShareHelper.writeShareImage(this, bitmap)
        val share = if (stories) Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/png")
        } else Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, link)
        }
        share.setPackage(target)
        share.clipData = ClipData.newRawUri("Card NERF Music", uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (stories) {
            grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            copyLink(link)
        }
        startActivity(share)
        if (stories) message("Link copiado. Adicione o adesivo de link no Instagram.")
    }

    private suspend fun writeBitmap(uri: Uri, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val stream = contentResolver.openOutputStream(uri) ?: error("Não foi possível abrir a imagem")
        stream.use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private suspend fun saveToGallery(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "NERF_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NERF Music")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Falha ao salvar")
        try {
            writeBitmap(uri, bitmap)
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } catch (error: Exception) {
            contentResolver.delete(uri, null, null)
            throw error
        }
    }
}

@Composable
private fun SharePreviewSheet(
    track: Track, style: SocialShareHelper.Style, bitmap: Bitmap?, error: String?, busy: Boolean,
    visible: Boolean, onClose: () -> Unit, onStyleChange: (SocialShareHelper.Style) -> Unit,
    onInstagram: () -> Unit, onWhatsApp: () -> Unit, onSave: () -> Unit, onCopy: () -> Unit
) {
    val enabled = bitmap != null && !busy
    Box(Modifier.fillMaxSize().background(Color(0xFF08090B))) {
        AsyncImage(track.highResThumbnailUrl ?: track.thumbnailUrl, null,
            Modifier.fillMaxWidth().height(250.dp).alpha(0.25f), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(250.dp).background(Color.Black.copy(alpha = 0.56f)))
        Column(Modifier.statusBarsPadding().padding(start = 28.dp, top = 38.dp)) {
            Text(track.title.uppercase(), color = Color.White.copy(alpha = 0.82f), fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(track.artist, color = Color.White.copy(alpha = 0.62f), fontSize = 14.sp, maxLines = 1)
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(280, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f))) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(180, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f))) { it / 3 } + fadeOut(tween(120))
        ) {
            Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.84f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF111116)).border(1.dp, GlassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleAction({}, false, 46.dp) { Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(23.dp)) }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Compartilhar música", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Crie um card para esta faixa", color = Color.White.copy(alpha = 0.60f), fontSize = 12.sp)
                }
                CircleAction(onClose, size = 36.dp) { Icon(Icons.Outlined.Close, "Fechar", tint = Color.White, modifier = Modifier.size(21.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            DividerLine()
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(track.highResThumbnailUrl ?: track.thumbnailUrl, null, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = Color.White.copy(alpha = 0.60f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(24.dp).clip(CircleShape).background(ShareOrange), contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("PRÉVIA")
                Text("1080 × 1920", color = Color.White.copy(alpha = 0.46f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = bitmap,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.98f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.98f))
                    },
                    label = "shareCardStyle"
                ) { card ->
                    when {
                        card != null -> Image(card.asImageBitmap(), "Prévia de ${track.title}", Modifier.width(172.dp).height(306.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
                        error != null -> Text(error, color = Color.White.copy(alpha = 0.68f), textAlign = TextAlign.Center, modifier = Modifier.height(306.dp).padding(32.dp))
                        else -> Box(Modifier.width(172.dp).height(306.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ShareOrange, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("ESTILO DO CARD")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SocialShareHelper.Style.entries.forEach { option ->
                    StyleOption(option, option == style, !busy, { onStyleChange(option) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            DividerLine()
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ShareAction("Stories", R.drawable.ic_social_instagram, ShareOrange, enabled, onInstagram)
                ShareAction("WhatsApp", R.drawable.ic_social_whatsapp, Color.White.copy(alpha = 0.10f), enabled, onWhatsApp)
                ShareAction("Salvar", R.drawable.ic_social_download, Color.White.copy(alpha = 0.10f), enabled, onSave)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).applePressFeedback(!busy, 0.98f, onCopy),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ContentCopy, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar link da música", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}


@Composable
private fun DividerLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))

@Composable
private fun SectionLabel(text: String) = Text(text, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
private fun StyleOption(style: SocialShareHelper.Style, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = when (style) {
        SocialShareHelper.Style.GLASS -> "Vidro"
        SocialShareHelper.Style.MINIMAL -> "Minimal"
        SocialShareHelper.Style.FULL_COVER -> "Capa inteira"
    }
    Box(
        modifier.height(58.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) ShareOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ShareOrange else GlassBorder, RoundedCornerShape(16.dp))
            .applePressFeedback(enabled, 0.97f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color.White.copy(alpha = 0.64f), fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShareAction(label: String, iconRes: Int, fill: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleAction(onClick, enabled, 56.dp, fill) {
            Icon(painterResource(iconRes), label, tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = if (enabled) 0.80f else 0.42f), fontSize = 12.sp)
    }
}

@Composable
private fun CircleAction(onClick: () -> Unit, enabled: Boolean = true, size: Dp, fill: Color = Color.White.copy(alpha = 0.10f), content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(fill).border(1.dp, GlassBorder, CircleShape).applePressFeedback(enabled, 0.97f, onClick), contentAlignment = Alignment.Center, content = content)
}
