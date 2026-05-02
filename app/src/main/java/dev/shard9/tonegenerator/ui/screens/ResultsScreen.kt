package dev.shard9.tonegenerator.ui.screens

import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen(viewModel: AppViewModel) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Results History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.history.isEmpty()) {
            Text("No results yet", color = Color.Gray)
        } else {
            viewModel.history.forEach { line ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = line,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val header = viewModel.getCSVHeader()
                    val allText = viewModel.history.joinToString("\n")
                    val csvData = "$header\n$allText"
                    scope.launch {
                        clipboard.setClipEntry(ClipData.newPlainText("Tone History", csvData).toClipEntry())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy All to Clipboard")
            }
        }
    }
}
