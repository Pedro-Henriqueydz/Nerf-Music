package com.nerf.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.music.ui.theme.*

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YtmBlack)
    ) {
        YtmTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LibraryMusic,
                contentDescription = "Biblioteca",
                tint = YtmWhite,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Em breve",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = YtmWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sua biblioteca de músicas curtidas, playlists salvas e downloads estará disponível aqui.",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 14.sp,
                color = YtmTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nerf Music v${com.nerf.music.BuildConfig.VERSION_NAME}",
                fontFamily = YouTubeSansFontFamily,
                fontSize = 12.sp,
                color = OrangePrimary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
