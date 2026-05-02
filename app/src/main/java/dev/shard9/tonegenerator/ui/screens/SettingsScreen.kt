package dev.shard9.tonegenerator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shard9.tonegenerator.ThemeMode
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Theme Mode", fontWeight = FontWeight.SemiBold)
        val themeModes = listOf(ThemeMode.LIGHT, ThemeMode.AUTO, ThemeMode.DARK)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            themeModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themeModes.size),
                    onClick = { viewModel.updateTheme(mode) },
                    selected = viewModel.themeMode == mode
                ) {
                    Text(
                        mode.name.lowercase().replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Frequency Range (Hz)", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.minFreq.toInt().toString(),
                onValueChange = {
                    val clean = it.filter { char -> char.isDigit() }
                    val i = clean.toIntOrNull()
                    if (i != null) {
                        val clamped = i.coerceIn(10, 30000)
                        if (clamped < viewModel.maxFreq) {
                            viewModel.updateFreqRange(clamped.toFloat(), viewModel.maxFreq)
                        }
                    }
                },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = viewModel.maxFreq.toInt().toString(),
                onValueChange = {
                    val clean = it.filter { char -> char.isDigit() }
                    val i = clean.toIntOrNull()
                    if (i != null) {
                        val clamped = i.coerceIn(10, 30000)
                        if (clamped > viewModel.minFreq) {
                            viewModel.updateFreqRange(viewModel.minFreq, clamped.toFloat())
                        }
                    }
                },
                label = { Text("Max") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Number of Positions: ${viewModel.positionCount}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = viewModel.positionCount.toFloat(),
            onValueChange = { viewModel.updatePositionCount(it.toInt()) },
            valueRange = 1f..6f,
            steps = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Position Names:", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        for (i in 0 until viewModel.positionCount) {
            OutlinedTextField(
                value = viewModel.positionNames[i],
                onValueChange = { viewModel.updatePositionName(i, it) },
                label = { Text("Position ${i + 1} Name") },
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.resetToDefaults() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Reset to Defaults")
        }
    }
}
