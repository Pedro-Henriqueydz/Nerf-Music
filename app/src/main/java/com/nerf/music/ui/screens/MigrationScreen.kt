package com.nerf.music.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MigrationProgressState
import com.nerf.music.ui.viewmodel.MusicViewModel

private val BgBlack = Color(0xFF121212)
private val CardDarkGray = Color(0xFF242424)
private val AccentGreen = Color(0xFF1DB954)
private val TextLightGray = Color(0xFFB3B3B3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isYtmConnected by viewModel.isYouTubeMusicConnected.collectAsState()
    val isSpotifyConnected by viewModel.isSpotifyConnected.collectAsState()
    val migrationProgress by viewModel.migrationProgress.collectAsState()

    var playlistUrlInput by remember { mutableStateOf("") }
    var showWebLoginDialog by remember { mutableStateOf<String?>(null) } // "ytm" ou "spotify"

    // Launcher para selecionar arquivo CSV / JSON / TXT
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (content.isNotBlank()) {
                    viewModel.importFromTextOrCsv(content, "Backup Importado")
                } else {
                    Toast.makeText(context, "O arquivo selecionado está vazio.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao ler arquivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Importar Biblioteca e Migração",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBlack)
            )
        },
        containerColor = BgBlack
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // -------------------------------------------------------------------------------------
            // CARD DE PRIVACIDADE E SEGURANÇA
            // -------------------------------------------------------------------------------------
            PrivacyBadgeCard()

            Spacer(modifier = Modifier.height(20.dp))

            // -------------------------------------------------------------------------------------
            // SEÇÃO 1: MODO CONEXÃO DIRETA (COM LOGIN)
            // -------------------------------------------------------------------------------------
            SectionHeader(
                title = "Conexão Direta (Recomendado)",
                subtitle = "Sincroniza suas Músicas Curtidas, Playlists e Artistas de uma só vez com 1 toque."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Card YouTube Music
            ConnectedServiceCard(
                serviceName = "YouTube Music / Google",
                iconColor = Color(0xFFFF0000),
                icon = Icons.Default.PlayCircleFilled,
                isConnected = isYtmConnected,
                connectedSubtitle = "Conta vinculada • Curtidas e playlists sincronizadas",
                disconnectedSubtitle = "Importe todas as suas Músicas Curtidas e Playlists do YouTube Music",
                onConnectClick = { showWebLoginDialog = "ytm" },
                onSyncClick = { viewModel.syncYouTubeMusicAccount() },
                onDisconnectClick = { viewModel.disconnectYouTubeMusicAccount() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card Spotify
            ConnectedServiceCard(
                serviceName = "Spotify",
                iconColor = Color(0xFF1DB954),
                icon = Icons.Default.MusicNote,
                isConnected = isSpotifyConnected,
                connectedSubtitle = "Conta vinculada • Liked Songs e playlists salvas",
                disconnectedSubtitle = "Traga suas Músicas Curtidas e Playlists favoritas do Spotify",
                onConnectClick = { showWebLoginDialog = "spotify" },
                onSyncClick = { viewModel.syncSpotifyAccount() },
                onDisconnectClick = { viewModel.disconnectSpotifyAccount() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // -------------------------------------------------------------------------------------
            // SEÇÃO 2: MODO ANÔNIMO (SEM LOGIN)
            // -------------------------------------------------------------------------------------
            SectionHeader(
                title = "Modo Anônimo (Sem Login)",
                subtitle = "Importe playlists públicas ou arquivos de backup sem precisar vincular nenhuma conta."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card: Colar Link Público de Playlist
            AnonymousLinkCard(
                urlInput = playlistUrlInput,
                onUrlChange = { playlistUrlInput = it },
                onPasteClipboard = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null && clipboard.hasPrimaryClip() &&
                        (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                                clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true)
                    ) {
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
                        if (text.startsWith("http")) {
                            playlistUrlInput = text
                            Toast.makeText(context, "Link colado da área de transferência!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nenhum link web encontrado no clipboard.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "A área de transferência está vazia.", Toast.LENGTH_SHORT).show()
                    }
                },
                onImportClick = {
                    val trimmed = playlistUrlInput.trim()
                    if (trimmed.contains("spotify.com")) {
                        viewModel.importFromSpotifyUrl(trimmed)
                        playlistUrlInput = ""
                    } else if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
                        viewModel.importFromYouTubeMusicUrl(trimmed)
                        playlistUrlInput = ""
                    } else {
                        Toast.makeText(context, "Insira um link válido do Spotify ou YouTube Music.", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card: Importar Arquivo de Backup
            AnonymousFileCard(
                onSelectFileClick = {
                    filePickerLauncher.launch("*/*")
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // MODAL DE PROGRESSO DE IMPORTAÇÃO
    // ---------------------------------------------------------------------------------------------
    migrationProgress?.let { progress ->
        if (progress.isImporting) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDarkGray),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = AccentGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Importando e Associando...",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (progress.total > 0) {
                            Text(
                                text = "Localizando no catálogo: (${progress.current} de ${progress.total})",
                                fontFamily = YouTubeSansFontFamily,
                                color = TextLightGray,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = AccentGreen,
                                trackColor = Color(0xFF333333)
                            )
                        }

                        if (progress.currentTrackTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progress.currentTrackTitle,
                                fontFamily = YouTubeSansFontFamily,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // MODAL WEBVIEW PARA LOGIN SEGURO (GOOGLE OU SPOTIFY)
    // ---------------------------------------------------------------------------------------------
    showWebLoginDialog?.let { target ->
        val loginUrl = if (target == "ytm") {
            "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"
        } else {
            "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
        }

        val serviceTitle = if (target == "ytm") "Google / YouTube Music" else "Spotify"

        Dialog(
            onDismissRequest = { showWebLoginDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BgBlack
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Login Oficial - $serviceTitle",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Página oficial e segura • Suporte a 2FA",
                                    fontFamily = YouTubeSansFontFamily,
                                    color = TextLightGray,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showWebLoginDialog = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardDarkGray)
                    )

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val uri = request?.url ?: return false
                                        val host = uri.host?.lowercase() ?: return true

                                        // Whitelist estrita de domínios confiáveis para autenticação OAuth/SSO
                                        val isAllowed = when (target) {
                                            "ytm" -> host.endsWith("google.com") || host.endsWith("youtube.com") ||
                                                    host.endsWith("gstatic.com") || host.endsWith("googleusercontent.com")
                                            "spotify" -> host.endsWith("spotify.com") || host.endsWith("scdn.co") ||
                                                    host.endsWith("spotifycdn.com")
                                            else -> false
                                        }

                                        return !isAllowed
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url == null) return

                                        if (target == "ytm") {
                                            val cookies = cookieManager.getCookie("https://music.youtube.com") ?: ""
                                            if (cookies.contains("SAPISID") || cookies.contains("SSID") || cookies.contains("__Secure-3PAPISID")) {
                                                viewModel.connectYouTubeMusicAccount(cookies, "Conta Google Conectada")
                                                showWebLoginDialog = null
                                                Toast.makeText(ctx, "Conectado ao YouTube Music com sucesso!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else if (target == "spotify") {
                                            val cookies = cookieManager.getCookie("https://open.spotify.com") ?: ""
                                            if (cookies.contains("sp_dc") || cookies.contains("sp_t")) {
                                                viewModel.connectSpotifyAccount(cookies, "Conta Spotify Conectada")
                                                showWebLoginDialog = null
                                                Toast.makeText(ctx, "Conectado ao Spotify com sucesso!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }

                                loadUrl(loginUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPONENTES MODULARES
// -------------------------------------------------------------------------------------------------

@Composable
private fun PrivacyBadgeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141E18)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DB954).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Segurança",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Privacidade e Segurança Garantidas",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O login é realizado diretamente nas páginas oficiais do Google e Spotify. O NERF Music nunca tem acesso à sua senha pura. Apenas lemos suas músicas e playlists para personalizar seu aplicativo. Suas credenciais ficam salvas e criptografadas localmente no seu aparelho.",
                    fontFamily = YouTubeSansFontFamily,
                    color = TextLightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = title,
            fontFamily = YouTubeSansFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            fontFamily = YouTubeSansFontFamily,
            color = TextLightGray,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ConnectedServiceCard(
    serviceName: String,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isConnected: Boolean,
    connectedSubtitle: String,
    disconnectedSubtitle: String,
    onConnectClick: () -> Unit,
    onSyncClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = serviceName,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = serviceName,
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) connectedSubtitle else disconnectedSubtitle,
                        fontFamily = YouTubeSansFontFamily,
                        color = if (isConnected) Color(0xFF1DB954) else TextLightGray,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDisconnectClick) {
                        Text(
                            text = "Desconectar",
                            fontFamily = YouTubeSansFontFamily,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onSyncClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizar",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sincronizar",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Conectar com $serviceName",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AnonymousLinkCard(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onImportClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Colar Link Público de Playlist",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cole o link de uma playlist pública do Spotify ou YouTube Music:",
                fontFamily = YouTubeSansFontFamily,
                color = TextLightGray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(
                        text = "https://open.spotify.com/playlist/... ou link do YouTube",
                        color = Color(0xFF777777),
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                trailingIcon = {
                    if (urlInput.isNotBlank()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Nota: No modo anônimo, o Spotify limita links públicos da web a 100 faixas. Para playlists completas com mais de 100 músicas, conecte sua conta Spotify acima ou importe um arquivo .csv/.txt.",
                fontFamily = YouTubeSansFontFamily,
                color = Color(0xFF999999),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPasteClipboard,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color(0xFF555555), Color(0xFF555555))))
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Colar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Colar do Clipboard", fontSize = 12.sp, fontFamily = YouTubeSansFontFamily)
                }

                Button(
                    onClick = onImportClick,
                    enabled = urlInput.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, disabledContainerColor = Color(0xFF333333)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Importar", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp, fontFamily = YouTubeSansFontFamily)
                }
            }
        }
    }
}

@Composable
private fun AnonymousFileCard(
    onSelectFileClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Importar Arquivo de Backup",
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selecione um arquivo .csv, .json ou texto puro (.txt) gerado pelo Soundiiz, TuneMyMusic ou backup manual:",
                fontFamily = YouTubeSansFontFamily,
                color = TextLightGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSelectFileClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color(0xFF555555), Color(0xFF555555)))),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = "Selecionar Arquivo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selecionar Arquivo no Aparelho",
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
