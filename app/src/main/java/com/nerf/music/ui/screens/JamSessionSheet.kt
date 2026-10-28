package com.nerf.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.jam.JamConnectionStatus
import com.nerf.music.jam.JamSessionState
import com.nerf.music.ui.components.JamPersonWavesIcon
import com.nerf.music.ui.components.applePressFeedback
import com.nerf.music.ui.theme.OrangePrimary
import com.nerf.music.ui.theme.SfProFontFamily
import com.nerf.music.ui.theme.YouTubeSansFontFamily
import com.nerf.music.ui.theme.YtmTextSecondary
import com.nerf.music.ui.theme.YtmWhite
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamSessionSheet(
    jamState: JamSessionState,
    onStartHostJam: (customRelayUrl: String?) -> Unit,
    onJoinJamInput: (input: String) -> Unit,
    onLeaveJam: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onToggleAllowGuestsToQueue: (Boolean) -> Unit = {},
    onToggleAllowGuestsToControlPlayback: (Boolean) -> Unit = {},
    onSelectRepeatMode: (String) -> Unit = {},
    onChangeHostCrossfadeSeconds: (Int) -> Unit = {},
    onToggleSyncKaraokeWithRoom: (Boolean) -> Unit = {},
    onKickParticipant: (String) -> Unit = {},
    onRequestAudioResync: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember(jamState.localDisplayName) { mutableStateOf(jamState.localDisplayName) }
    var joinLinkOrIpInput by remember { mutableStateOf("") }
    var showAdvancedRelayInput by remember { mutableStateOf(false) }
    var customRelayInput by remember { mutableStateOf("") }
    var showInviteQrModal by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "JamPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "JamPulseScale"
    )

    val JamAccentGreen = Color(0xFF1ED760)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        contentColor = YtmWhite,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to (if (jamState.isInJam) Color(0xFF132B1E) else Color(0xFF1E1E24)),
                        0.14f to (if (jamState.isInJam) Color(0xFF0F1E16) else Color(0xFF15151A)),
                        0.38f to Color(0xFF111114),
                        1.0f to Color(0xFF09090B)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. Cabeçalho Glass da JAM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (jamState.isInJam) JamAccentGreen.copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    0.75.dp,
                                    if (jamState.isInJam) JamAccentGreen.copy(alpha = 0.40f)
                                    else Color.White.copy(alpha = 0.14f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            JamPersonWavesIcon(
                                tint = if (jamState.isInJam) JamAccentGreen else Color.White,
                                size = 24.dp,
                                modifier = if (jamState.isInJam) Modifier.scale(pulseScale) else Modifier
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (jamState.isInJam) "NERF Jam ao vivo" else "NERF Jam",
                                fontFamily = SfProFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = YtmWhite
                            )
                            Text(
                                text = if (jamState.isInJam) {
                                    "${jamState.participants.size} dispositivo(s) em sincronia"
                                } else {
                                    "Escuta sincronizada em tempo real via WebSocket"
                                },
                                fontFamily = SfProFontFamily,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.60f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                            .applePressFeedback(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Identidade Efêmera Glass (Apelido do Dispositivo Sem Conta)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.12f)),
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
                                label = { Text("Seu apelido na Jam", fontFamily = SfProFontFamily) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = YtmWhite,
                                    unfocusedTextColor = YtmWhite,
                                    focusedBorderColor = JamAccentGreen,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(JamAccentGreen.copy(alpha = 0.20f))
                                    .border(0.75.dp, JamAccentGreen.copy(alpha = 0.50f), CircleShape)
                                    .applePressFeedback {
                                        onUpdateDisplayName(nameInput)
                                        isEditingName = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Salvar apelido", tint = JamAccentGreen, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Visível como (sem conta)",
                                        fontFamily = SfProFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                    Text(
                                        text = jamState.localDisplayName,
                                        fontFamily = SfProFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = YtmWhite
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                                    .applePressFeedback { isEditingName = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar apelido",
                                    tint = Color.White.copy(alpha = 0.80f),
                                    modifier = Modifier.size(16.dp)
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
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.75.dp, JamAccentGreen.copy(alpha = 0.35f)),
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
                                fontFamily = SfProFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = YtmWhite
                            )
                            Text(
                                text = "Conecta automaticamente via WebSocket Global (Internet / 4G / 5G / Wi-Fi) para você ouvir em tempo real com amigos em qualquer lugar do mundo usando um código de sala ou DeepLink.",
                                fontFamily = SfProFontFamily,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 18.sp
                            )

                            AnimatedVisibility(visible = showAdvancedRelayInput) {
                                OutlinedTextField(
                                    value = customRelayInput,
                                    onValueChange = { customRelayInput = it },
                                    placeholder = {
                                        Text("Opcional: 'local' (só Wi-Fi) ou wss://seu-relay", fontFamily = SfProFontFamily, color = Color.White.copy(alpha = 0.40f), fontSize = 13.sp)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = YtmWhite,
                                        unfocusedTextColor = YtmWhite,
                                        focusedBorderColor = JamAccentGreen,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.10f)),
                                    modifier = Modifier.applePressFeedback {
                                        showAdvancedRelayInput = !showAdvancedRelayInput
                                    }
                                ) {
                                    Text(
                                        text = if (showAdvancedRelayInput) "Relay Padrão" else "Avançado / LAN",
                                        fontFamily = SfProFontFamily,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = JamAccentGreen,
                                    modifier = Modifier.applePressFeedback {
                                        onStartHostJam(customRelayInput.takeIf { showAdvancedRelayInput && it.isNotBlank() })
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        JamPersonWavesIcon(
                                            tint = Color.Black,
                                            size = 18.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Start Jam",
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card Glass para Entrar em uma Jam existente
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.12f)),
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
                                fontFamily = SfProFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = YtmWhite
                            )
                            Text(
                                text = "Digite apenas o código da sala (ex: NERF-4821 ou 4821) para conectar de qualquer lugar via Internet, ou cole o DeepLink de convite:",
                                fontFamily = SfProFontFamily,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = joinLinkOrIpInput,
                                onValueChange = { joinLinkOrIpInput = it },
                                placeholder = {
                                    Text(
                                        text = "Código (ex: NERF-4821) ou link...",
                                        fontFamily = SfProFontFamily,
                                        color = Color.White.copy(alpha = 0.40f),
                                        fontSize = 13.sp
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(0.75.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                            .applePressFeedback {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                                if (clipText.isNotBlank()) {
                                                    joinLinkOrIpInput = clipText.trim()
                                                } else {
                                                    Toast.makeText(context, "Área de transferência vazia", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Colar convite",
                                            tint = YtmWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = YtmWhite,
                                    unfocusedTextColor = YtmWhite,
                                    focusedBorderColor = JamAccentGreen,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (joinLinkOrIpInput.isNotBlank()) Color.White else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.75.dp, if (joinLinkOrIpInput.isNotBlank()) Color.White else Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .applePressFeedback(enabled = joinLinkOrIpInput.isNotBlank()) {
                                        if (joinLinkOrIpInput.isNotBlank()) {
                                            onJoinJamInput(joinLinkOrIpInput.trim())
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = if (joinLinkOrIpInput.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.40f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Conectar via WebSocket",
                                        fontFamily = SfProFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (joinLinkOrIpInput.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.40f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // =========================================================================
                    // MODO CONECTADO: PAINEL DA SESSÃO ATIVA + CONVITE DEEPLINK + PARTICIPANTES
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF14241B).copy(alpha = 0.85f),
                        border = BorderStroke(0.75.dp, JamAccentGreen.copy(alpha = 0.45f)),
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
                                        fontFamily = SfProFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JamAccentGreen
                                    )
                                    Text(
                                        text = if (jamState.isHost) "Você é o Host desta Jam" else "Hospedada por ${jamState.hostName}",
                                        fontFamily = SfProFontFamily,
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
                                    color = JamAccentGreen.copy(alpha = 0.20f),
                                    border = BorderStroke(0.75.dp, JamAccentGreen.copy(alpha = 0.40f))
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontFamily = SfProFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JamAccentGreen,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Código da Sala em destaque + Endpoint WebSocket e DeepLink
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.40f),
                                border = BorderStroke(0.75.dp, JamAccentGreen.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .applePressFeedback {
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
                                                fontFamily = SfProFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.55f)
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
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copiar código",
                                                tint = YtmWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
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
                                    fontFamily = SfProFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6B6B)
                                )
                            }

                            // Botões de Convite por DeepLink (Compartilhar / Copiar / QR)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = JamAccentGreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .applePressFeedback {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Entre na minha Jam no NERF Music para ouvirmos juntos em tempo real de qualquer lugar!\n\nCódigo da Sala: ${jamState.sessionId}\nOu toque no link: ${jamState.inviteDeepLink}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Convidar para a Jam"))
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartilhar DeepLink",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Convidar",
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Black
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
                                    modifier = Modifier.applePressFeedback {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("NERF Jam DeepLink", jamState.inviteDeepLink))
                                        Toast.makeText(context, "DeepLink copiado!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copiar DeepLink",
                                            tint = YtmWhite,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Copiar",
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = YtmWhite
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (showInviteQrModal) JamAccentGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            0.75.dp,
                                            if (showInviteQrModal) JamAccentGreen.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.16f),
                                            CircleShape
                                        )
                                        .applePressFeedback { showInviteQrModal = !showInviteQrModal },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Código visual",
                                        tint = if (showInviteQrModal) JamAccentGreen else YtmWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showInviteQrModal) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
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

                    // Configurações da Sala e Sincronia de Áudio Glass (Host e Convidados)
                    val roomSettings = jamState.roomSettings
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = JamAccentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (jamState.isHost) {
                                            "Configurações da Sala"
                                        } else {
                                            "Regras e Áudio da Sala"
                                        },
                                        fontFamily = SfProFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = YtmWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = JamAccentGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(0.75.dp, JamAccentGreen.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = if (roomSettings.hostCrossfadeSeconds > 0) {
                                            "${roomSettings.hostCrossfadeSeconds}s"
                                        } else {
                                            "Off"
                                        },
                                        fontFamily = SfProFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JamAccentGreen,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (jamState.isHost) {
                                // 1. Permissão de Fila
                                JamSettingToggleRow(
                                    icon = Icons.Default.QueueMusic,
                                    title = "Todos podem colocar músicas",
                                    subtitle = "Convidados podem adicionar, remover e reordenar a fila",
                                    checked = roomSettings.allowGuestsToQueue,
                                    accentColor = JamAccentGreen,
                                    onCheckedChange = onToggleAllowGuestsToQueue
                                )

                                // 2. Permissão de Pular / Pausar / Seek
                                JamSettingToggleRow(
                                    icon = Icons.Default.SkipNext,
                                    title = "Todos podem pular e pausar",
                                    subtitle = "Convidados podem avançar, voltar, pausar e escolher faixa",
                                    checked = roomSettings.allowGuestsToControlPlayback,
                                    accentColor = JamAccentGreen,
                                    onCheckedChange = onToggleAllowGuestsToControlPlayback
                                )

                                // 3. Looping Sincronizado da Sala
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (roomSettings.repeatModeName == "ONE") {
                                                Icons.Default.RepeatOne
                                            } else {
                                                Icons.Default.Repeat
                                            },
                                            contentDescription = null,
                                            tint = if (roomSettings.repeatModeName != "OFF") JamAccentGreen else Color.White.copy(alpha = 0.60f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Looping da Jam",
                                                fontFamily = SfProFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = YtmWhite
                                            )
                                            Text(
                                                text = "Sincronizado para todos os participantes",
                                                fontFamily = SfProFontFamily,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.55f)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val repeatOptions = listOf(
                                            "OFF" to "Desativado",
                                            "ALL" to "Repetir Fila",
                                            "ONE" to "Repetir 1"
                                        )
                                        repeatOptions.forEach { (modeKey, modeLabel) ->
                                            val isSelected = roomSettings.repeatModeName == modeKey
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (isSelected) JamAccentGreen else Color.White.copy(alpha = 0.06f),
                                                border = BorderStroke(
                                                    0.75.dp,
                                                    if (isSelected) JamAccentGreen else Color.White.copy(alpha = 0.12f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .applePressFeedback { onSelectRepeatMode(modeKey) }
                                            ) {
                                                Text(
                                                    text = modeLabel,
                                                    fontFamily = SfProFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.Black else YtmWhite,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. Crossfade do Host (Aplicado a Todos)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Crossfade da Jam (aplicado a todos)",
                                                fontFamily = SfProFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = YtmWhite
                                            )
                                            Text(
                                                text = "Todos na sala usam o mesmo tempo de transição do Host",
                                                fontFamily = SfProFontFamily,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.55f)
                                            )
                                        }
                                        Text(
                                            text = if (roomSettings.hostCrossfadeSeconds == 0) {
                                                "Off"
                                            } else {
                                                "${roomSettings.hostCrossfadeSeconds}s"
                                            },
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = JamAccentGreen
                                        )
                                    }

                                    Slider(
                                        value = roomSettings.hostCrossfadeSeconds.toFloat(),
                                        onValueChange = { value ->
                                            onChangeHostCrossfadeSeconds(value.roundToInt().coerceIn(0, 12))
                                        },
                                        valueRange = 0f..12f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(
                                            thumbColor = JamAccentGreen,
                                            activeTrackColor = JamAccentGreen,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 5. Sincronizar Karaokê com a Sala
                                JamSettingToggleRow(
                                    icon = Icons.Default.Mic,
                                    title = "Sincronizar Karaokê com a sala",
                                    subtitle = "Ativa o instrumental e nível de voz em todos os aparelhos",
                                    checked = roomSettings.syncKaraokeWithRoom,
                                    accentColor = JamAccentGreen,
                                    onCheckedChange = onToggleSyncKaraokeWithRoom
                                )
                            } else {
                                // Visão do Convidado (Guest)
                                val repeatText = when (roomSettings.repeatModeName) {
                                    "ONE" -> "Repetindo 1 música"
                                    "ALL" -> "Repetindo toda a fila"
                                    else -> "Sem repetição"
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "• Fila colaborativa: ${if (roomSettings.allowGuestsToQueue) "Liberada para todos" else "Apenas o Host"}",
                                        fontFamily = SfProFontFamily,
                                        fontSize = 12.sp,
                                        color = YtmWhite
                                    )
                                    Text(
                                        text = "• Pular / Pausar músicas: ${if (roomSettings.allowGuestsToControlPlayback) "Liberado para todos" else "Apenas o Host"}",
                                        fontFamily = SfProFontFamily,
                                        fontSize = 12.sp,
                                        color = YtmWhite
                                    )
                                    Text(
                                        text = "• Looping da sala: $repeatText",
                                        fontFamily = SfProFontFamily,
                                        fontSize = 12.sp,
                                        color = YtmWhite
                                    )
                                    Text(
                                        text = "• Crossfade sincronizado do Host: ${if (roomSettings.hostCrossfadeSeconds > 0) "${roomSettings.hostCrossfadeSeconds}s ativo no seu player" else "Desativado (0s)"}",
                                        fontFamily = SfProFontFamily,
                                        fontSize = 12.sp,
                                        color = JamAccentGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (roomSettings.syncKaraokeWithRoom) {
                                        Text(
                                            text = "• Modo Karaokê sincronizado pelo Host",
                                            fontFamily = SfProFontFamily,
                                            fontSize = 12.sp,
                                            color = OrangePrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = JamAccentGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(0.75.dp, JamAccentGreen.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .applePressFeedback(onClick = onRequestAudioResync)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Ressincronizar áudio",
                                            tint = JamAccentGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ressincronizar Áudio com o Host",
                                            fontFamily = SfProFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = JamAccentGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Lista de Participantes na Sessão Glass
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.12f)),
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
                                    fontFamily = SfProFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = YtmWhite
                                )
                            }

                            jamState.participants.forEach { participant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
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
                                                fontFamily = SfProFontFamily,
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
                                                    fontFamily = SfProFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = YtmWhite
                                                )
                                                if (participant.peerId == jamState.localPeerId) {
                                                    Text(
                                                        text = "(Você)",
                                                        fontFamily = SfProFontFamily,
                                                        fontSize = 12.sp,
                                                        color = Color.White.copy(alpha = 0.55f)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (participant.isHost) "Host da Jam" else "Convidado sincronizado",
                                                fontFamily = SfProFontFamily,
                                                fontSize = 11.sp,
                                                color = if (participant.isHost) JamAccentGreen else Color.White.copy(alpha = 0.55f)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (participant.rttMs > 0L) {
                                            Text(
                                                text = "${participant.rttMs} ms",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = JamAccentGreen
                                            )
                                        }
                                        if (jamState.isHost && !participant.isHost && participant.peerId != jamState.localPeerId) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF453A).copy(alpha = 0.15f))
                                                    .border(0.75.dp, Color(0xFFFF453A).copy(alpha = 0.35f), CircleShape)
                                                    .applePressFeedback { onKickParticipant(participant.peerId) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonRemove,
                                                    contentDescription = "Remover participante",
                                                    tint = Color(0xFFFF453A),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Botão para Sair / Encerrar a Jam Glass
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFF453A).copy(alpha = 0.12f),
                        border = BorderStroke(0.75.dp, Color(0xFFFF453A).copy(alpha = 0.40f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .applePressFeedback(onClick = onLeaveJam)
                    ) {
                        Text(
                            text = if (jamState.isHost) "Encerrar sessão Jam para todos" else "Sair da Jam",
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFF453A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
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

@Composable
private fun JamSettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .applePressFeedback { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (checked) accentColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) accentColor else Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = YtmWhite
                )
                Text(
                    text = subtitle,
                    fontFamily = SfProFontFamily,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Switch iOS Fluido
        val thumbOffsetDp by animateDpAsState(
            targetValue = if (checked) 18.dp else 2.dp,
            animationSpec = tween(durationMillis = 160, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
            label = "JamSwitchThumb"
        )
        val trackColor by animateColorAsState(
            targetValue = if (checked) accentColor else Color.White.copy(alpha = 0.18f),
            animationSpec = tween(durationMillis = 160),
            label = "JamSwitchTrack"
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(start = thumbOffsetDp)
                    .size(22.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
