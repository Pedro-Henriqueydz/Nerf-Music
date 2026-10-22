package com.nerf.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.BuildConfig
import com.nerf.music.ui.theme.*
import com.nerf.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCanvasEnabled by viewModel.isCanvasEnabled.collectAsState()
    val isCanvasWifiOnly by viewModel.isCanvasWifiOnly.collectAsState()
    val isLyricsAboveTitleEnabled by viewModel.isLyricsAboveTitleEnabled.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack),
        containerColor = YtmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configurações",
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = YtmWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = YtmWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = YtmBlack
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção Reprodução & Visual
            SettingsSectionHeader(title = "Reprodução e Visual")

            SettingsSwitchItem(
                icon = Icons.Default.VideoLibrary,
                title = "Vídeo em segundo plano (Canvas)",
                subtitle = "Exibe o visualizer ou videoclipe no fundo do player expandido estilo Spotify Canvas",
                checked = isCanvasEnabled,
                onCheckedChange = { viewModel.setCanvasEnabled(it) }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Wifi,
                title = "Canvas apenas no Wi-Fi",
                subtitle = "Economiza dados móveis reproduzindo o vídeo de fundo somente conectado ao Wi-Fi",
                checked = isCanvasWifiOnly,
                enabled = isCanvasEnabled,
                onCheckedChange = { viewModel.setCanvasWifiOnly(it) }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Subtitles,
                title = "Letras sincronizadas no Player",
                subtitle = "Exibe a frase da letra em tempo real logo acima do título da faixa",
                checked = isLyricsAboveTitleEnabled,
                onCheckedChange = { viewModel.setLyricsAboveTitleEnabled(it) }
            )

            HorizontalDivider(
                color = YtmDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Seção Sobre & Atualizações
            SettingsSectionHeader(title = "Sobre o App")

            SettingsActionItem(
                icon = Icons.Default.SystemUpdate,
                title = "Versão do aplicativo",
                subtitle = "Nerf Music v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                onClick = {
                    viewModel.checkForAppUpdates(manual = true)
                }
            )

            SettingsActionItem(
                icon = Icons.Default.Palette,
                title = "Ver novidades da versão (Change Log)",
                subtitle = "Relembre os novos recursos adicionados nesta versão",
                onClick = {
                    viewModel.showChangeLogManually()
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = YouTubeSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = OrangePrimary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) YtmWhite else YtmTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (enabled) YtmWhite else YtmTextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 12.sp,
                color = if (enabled) YtmTextSecondary else YtmTextSecondary.copy(alpha = 0.35f),
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = YtmBlack,
                checkedTrackColor = OrangePrimary,
                uncheckedThumbColor = YtmTextSecondary,
                uncheckedTrackColor = YtmGrey11
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = YtmWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = YouTubeSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = YtmWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = YouTubeSansFontFamily,
                fontSize = 12.sp,
                color = YtmTextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
