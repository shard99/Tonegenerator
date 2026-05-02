package dev.shard9.tonegenerator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shard9.tonegenerator.BuildConfig


@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Tone Generator", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Developer: Andreas Østrem Nielsen")
        Text("GitHub: https://github.com/shard99/Tonegenerator")
        Spacer(modifier = Modifier.height(24.dp))
        Text("Version: ${BuildConfig.VERSION_NAME}")
        Text("Date: ${BuildConfig.BUILD_DATE}")
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "A precise tool for debugging low frequency room acoustics.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
        )
    }
}
