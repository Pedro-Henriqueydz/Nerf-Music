package com.nerf.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.jam.JamConnectionStatus
import com.nerf.music.jam.JamSessionState
import com.nerf.music.ui.theme.OrangePrimary
import com.nerf.music.ui.theme.YouTubeSansFontFamily
import com.nerf.music.ui.theme.YtmTextSecondary
import com.nerf.music.ui.theme.YtmWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamSessionSheet(
    jamState: JamSessionState,
    onStartHostJam: (customRelayUrl: String?) -> Unit,
    onJoinJamInput: (input: String) -> Unit,
    onLeaveJam: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember(jamState.localDisplayName) { mutableStateOf(jamState.localDisplayName) }
    var joinLinkOrIpInput by remember { mutableStateOf("") }
    var showAdvancedRelayInput by remember { mutableStateOf(false) }
    var customRelayInput by remember { mutableStateOf("") }
    var showInviteQrModal by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "JamPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "JamPulseScale"
    )

    val JamAccentGreen = Color(0xFF1ED760)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        contentColor = YtmWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Cabeçalho da JAM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (jamState.isInJam) {
                                    JamAccentGreen.copy(alpha = 0.2f)
                                } else {
                                    OrangePrimary.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (jamState.isInJam) Icons.Default.WifiTethering else Icons.Default.Podcasts,
                            contentDescription = "JAM",
                            tint = if (jamState.isInJam) JamAccentGreen else OrangePrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .then(if (jamState.isInJam) Modifier.scale(pulseScale) else Modifier)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (jamState.isInJam) "NERF Jam ao vivo" else "NERF Jam",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = YtmWhite
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "NO-ACCOUNT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (jamState.isInJam) JamAccentGreen else OrangePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = if (jamState.isInJam) {
                                "${jamState.participants.size} dispositivo(s) ouvindo em sincronia via WebSocket"
                            } else {
                                "Escuta sincronizada em tempo real via WebSocket e DeepLinks"
                            },
                            fontFamily = YouTubeSansFontFamily,
                            fontSize = 13.sp,
                            color = YtmTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = YtmTextSecondary
                    )
                }
            }

            // 2. Identidade Efêmera (Apelido do Dispositivo Sem Conta)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF222222),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            label = { Text("Seu apelido na Jam") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = YtmWhite,
                                unfocusedTextColor = YtmWhite,
                                focusedBorderColor = JamAccentGreen,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                onUpdateDisplayName(nameInput)
                                isEditingName = false
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Salvar apelido", tint = JamAccentGreen)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = YtmTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Visível como (sem conta)",
                                    fontSize = 11.sp,
                                    color = YtmTextSecondary
                                )
                                Text(
                                    text = jamState.localDisplayName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = YtmWhite
                                )
                            }
                        }
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar apelido",
                                tint = YtmTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (!jamState.isInJam) {
                // =========================================================================
                // MODO DESCONECTADO: CRIAR JAM (HOST) OU ENTRAR EM JAM (GUEST)
                // =========================================================================
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF202020),
                    border = BorderStroke(1.dp, JamAccentGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Hospedar uma sessão Jam Global",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = YtmWhite
                        )
                        Text(
                            text = "Conecta automaticamente via WebSocket Global (Internet / 4G / 5G / Wi-Fi) para você ouvir em tempo real com amigos em qualquer lugar do mundo usando um código de sala ou DeepLink.",
                            fontSize = 13.sp,
                            color = YtmTextSecondary,
                            lineHeight = 18.sp
                        )

                        AnimatedVisibility(visible = showAdvancedRelayInput) {
                            OutlinedTextField(
                                value = customRelayInput,
                                onValueChange = { customRelayInput = it },
                                placeholder = {
                                    Text("Opcional: 'local' (só Wi-Fi) ou wss://seu-relay", color = YtmTextSecondary, fontSize = 13.sp)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = YtmWhite,
                                    unfocusedTextColor = YtmWhite,
                                    focusedBorderColor = JamAccentGreen,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showAdvancedRelayInput = !showAdvancedRelayInput }
                            ) {
                                Text(
                                    text = if (showAdvancedRelayInput) "Usar Relay Global Padrão" else "Opções Avançadas / LAN",
                                    fontSize = 12.sp,
                                    color = YtmTextSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    onStartHostJam(customRelayInput.takeIf { showAdvancedRelayInput && it.isNotBlank() })
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JamAccentGreen,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Iniciar uma Jam",
                                    fontFamily = YouTubeSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Card para Entrar em uma Jam existente
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF202020),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Entrar na Jam de um amigo",
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = YtmWhite
                        )
                        Text(
                            text = "Digite apenas o código da sala (ex: NERF-4821 ou 4821) para conectar de qualquer lugar via Internet, ou cole o DeepLink de convite:",
                            fontSize = 13.sp,
                            color = YtmTextSecondary,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = joinLinkOrIpInput,
                            onValueChange = { joinLinkOrIpInput = it },
                            placeholder = {
                                Text(
                                    text = "Código (ex: NERF-4821) ou nerfmusic://jam/join?...",
                                    color = YtmTextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                        if (clipText.isNotBlank()) {
                                            joinLinkOrIpInput = clipText.trim()
                                        } else {
                                            Toast.makeText(context, "Área de transferência vazia", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Colar convite",
                                        tint = YtmWhite
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = YtmWhite,
                                unfocusedTextColor = YtmWhite,
                                focusedBorderColor = YtmWhite,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (joinLinkOrIpInput.isNotBlank()) {
                                    onJoinJamInput(joinLinkOrIpInput.trim())
                                }
                            },
                            enabled = joinLinkOrIpInput.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YtmWhite,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF333333),
                                disabledContentColor = YtmTextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conectar via WebSocket",
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                // =========================================================================
                // MODO CONECTADO: PAINEL DA SESSÃO ATIVA + CONVITE DEEPLINK + PARTICIPANTES
                // =========================================================================
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2822),
                    border = BorderStroke(1.dp, JamAccentGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SESSÃO ${jamState.sessionId}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JamAccentGreen
                                )
                                Text(
                                    text = if (jamState.isHost) "Você é o Host desta Jam" else "Hospedada por ${jamState.hostName}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YtmWhite
                                )
                            }

                            val statusLabel = when (jamState.connectionStatus) {
                                JamConnectionStatus.CONNECTED -> if (jamState.rttMs > 0) "AO VIVO • ${jamState.rttMs}ms" else "AO VIVO"
                                JamConnectionStatus.CONNECTING -> "CONECTANDO..."
                                JamConnectionStatus.RECONNECTING -> "RECONECTANDO..."
                                JamConnectionStatus.ERROR -> "ERRO"
                                JamConnectionStatus.DISCONNECTED -> "OFFLINE"
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = JamAccentGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JamAccentGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Código da Sala em destaque + Endpoint WebSocket e DeepLink
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.42f),
                            border = BorderStroke(0.5.dp, JamAccentGreen.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Código da Sala JAM", jamState.sessionId))
                                    Toast.makeText(context, "Código ${jamState.sessionId} copiado!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CÓDIGO DA SALA (INTERNET GLOBAL)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = YtmTextSecondary
                                        )
                                        Text(
                                            text = jamState.sessionId,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            color = JamAccentGreen,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = YtmWhite,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Copiar código",
                                                fontSize = 11.sp,
                                                color = YtmWhite
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Relay: ${jamState.wsEndpointUrl}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "DeepLink: ${jamState.inviteDeepLink}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = JamAccentGreen.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!jamState.errorMessage.isNullOrBlank()) {
                            Text(
                                text = jamState.errorMessage,
                                fontSize = 12.sp,
                                color = Color(0xFFFF6B6B)
                            )
                        }

                        // Botões de Convite por DeepLink (Compartilhar / Copiar / QR)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Entre na minha Jam no NERF Music para ouvirmos juntos em tempo real de qualquer lugar!\n\nCódigo da Sala: ${jamState.sessionId}\nOu toque no link: ${jamState.inviteDeepLink}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Convidar para a Jam"))
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JamAccentGreen,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartilhar DeepLink",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Convidar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("NERF Jam DeepLink", jamState.inviteDeepLink))
                                    Toast.makeText(context, "DeepLink copiado!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = YtmWhite)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar DeepLink",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showInviteQrModal = !showInviteQrModal },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = YtmWhite),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Código visual",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = showInviteQrModal) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    JamVisualMatrixBadge(seed = jamState.inviteDeepLink)
                                    Text(
                                        text = jamState.sessionId,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = jamState.wsEndpointUrl,
                                        color = Color.DarkGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Lista de Participantes na Sessão
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF202020),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = YtmWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Participantes na sessão (${jamState.participants.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = YtmWhite
                            )
                        }

                        jamState.participants.forEach { participant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = if (participant.isHost) {
                                                        listOf(JamAccentGreen, Color(0xFF0E8A3E))
                                                    } else {
                                                        listOf(OrangePrimary, Color(0xFF9C3D14))
                                                    }
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = participant.displayName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = participant.displayName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = YtmWhite
                                            )
                                            if (participant.peerId == jamState.localPeerId) {
                                                Text(
                                                    text = "(Você)",
                                                    fontSize = 12.sp,
                                                    color = YtmTextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (participant.isHost) "Host da Jam" else "Convidado sincronizado",
                                            fontSize = 11.sp,
                                            color = if (participant.isHost) JamAccentGreen else YtmTextSecondary
                                        )
                                    }
                                }

                                if (participant.rttMs > 0L) {
                                    Text(
                                        text = "${participant.rttMs} ms",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = JamAccentGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Botão para Sair / Encerrar a Jam
                OutlinedButton(
                    onClick = onLeaveJam,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (jamState.isHost) "Encerrar sessão Jam para todos" else "Sair da Jam",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun JamVisualMatrixBadge(seed: String) {
    val gridSize = 15
    val hashBytes = remember(seed) {
        java.security.MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
    }
    Canvas(modifier = Modifier.size(136.dp)) {
        val cell = size.width / gridSize
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val isFinderTopLeft = r < 4 && c < 4
                val isFinderTopRight = r < 4 && c >= gridSize - 4
                val isFinderBottomLeft = r >= gridSize - 4 && c < 4
                val isActive = if (isFinderTopLeft || isFinderTopRight || isFinderBottomLeft) {
                    val lr = if (r >= gridSize - 4) r - (gridSize - 4) else r
                    val lc = if (c >= gridSize - 4) c - (gridSize - 4) else c
                    lr == 0 || lr == 3 || lc == 0 || lc == 3 || (lr in 1..2 && lc in 1..2)
                } else {
                    val idx = (r * gridSize + c) % hashBytes.size
                    val bit = (r + c) % 8
                    ((hashBytes[idx].toInt() ushr bit) and 1) == 1
                }
                if (isActive) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell * 0.92f, cell * 0.92f)
                    )
                }
            }
        }
    }
}
