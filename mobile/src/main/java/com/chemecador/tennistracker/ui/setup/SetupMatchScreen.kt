package com.chemecador.tennistracker.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chemecador.tennistracker.scoring.FinalSetMode
import com.chemecador.tennistracker.scoring.MatchConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupMatchScreen(
    accountLabel: String,
    onStart: (MatchConfig) -> Unit,
    onOpenProfile: () -> Unit,
) {
    var bestOfSets by remember { mutableStateOf(3) }
    var finalSetMode by remember { mutableStateOf(FinalSetMode.TIEBREAK_7) }
    var goldenPoint by remember { mutableStateOf(false) }
    var advancedOptions by remember { mutableStateOf(false) }
    var playerA by remember { mutableStateOf("A") }
    var playerB by remember { mutableStateOf("B") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo partido") },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil",
                        )
                    }
                },
            )
        },
    ) { padding ->
        SetupContent(
            contentPadding = padding,
            accountLabel = accountLabel,
            playerA = playerA,
            playerB = playerB,
            bestOfSets = bestOfSets,
            finalSetMode = finalSetMode,
            goldenPoint = goldenPoint,
            advancedOptions = advancedOptions,
            onPlayerAChange = { playerA = it },
            onPlayerBChange = { playerB = it },
            onBestOfChange = { bestOfSets = it },
            onFinalSetModeChange = { finalSetMode = it },
            onGoldenPointChange = { goldenPoint = it },
            onAdvancedOptionsChange = { advancedOptions = it },
            onStart = {
                onStart(
                    MatchConfig(
                        bestOfSets = bestOfSets,
                        finalSetMode = finalSetMode,
                        goldenPoint = goldenPoint,
                        playerNameA = playerA.ifBlank { "A" },
                        playerNameB = playerB.ifBlank { "B" },
                    )
                )
            },
        )
    }
}

@Composable
private fun SetupContent(
    contentPadding: PaddingValues,
    accountLabel: String,
    playerA: String,
    playerB: String,
    bestOfSets: Int,
    finalSetMode: FinalSetMode,
    goldenPoint: Boolean,
    advancedOptions: Boolean,
    onPlayerAChange: (String) -> Unit,
    onPlayerBChange: (String) -> Unit,
    onBestOfChange: (Int) -> Unit,
    onFinalSetModeChange: (FinalSetMode) -> Unit,
    onGoldenPointChange: (Boolean) -> Unit,
    onAdvancedOptionsChange: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = accountLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionLabel("Jugadores")
        OutlinedTextField(
            value = playerA,
            onValueChange = onPlayerAChange,
            label = { Text("Jugador A") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = playerB,
            onValueChange = onPlayerBChange,
            label = { Text("Jugador B") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Opciones avanzadas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(checked = advancedOptions, onCheckedChange = onAdvancedOptionsChange)
        }

        if (advancedOptions) {
            SectionLabel("Sets a jugar")
            ChipRow(
                options = listOf(1, 3, 5),
                selected = bestOfSets,
                label = { "Al mejor de $it" },
                onSelect = onBestOfChange,
            )

            SectionLabel("Set decisivo")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinalSetMode.entries.forEach { mode ->
                    FilterChip(
                        selected = finalSetMode == mode,
                        onClick = { onFinalSetModeChange(mode) },
                        label = { Text(mode.displayName()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SectionLabel("Punto de oro")
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (goldenPoint) "Activado" else "Desactivado")
                Switch(checked = goldenPoint, onCheckedChange = onGoldenPointChange)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Empezar")
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun FinalSetMode.displayName(): String = when (this) {
    FinalSetMode.TIEBREAK_7 -> "Tie-break a 7"
    FinalSetMode.SUPER_TIEBREAK_10 -> "Super tie-break a 10"
    FinalSetMode.ADVANTAGE -> "Ventaja (sin tie-break)"
}
