package com.chemecador.tennistracker.wear.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.chemecador.tennistracker.scoring.FinalSetMode
import com.chemecador.tennistracker.scoring.MatchConfig
import com.chemecador.tennistracker.scoring.Sport
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

@Composable
fun SetupMatchScreen(onStart: (MatchConfig) -> Unit) {
    var sport by remember { mutableStateOf(Sport.TENNIS) }
    var bestOfSets by remember { mutableStateOf(3) }
    var finalSetMode by remember { mutableStateOf(FinalSetMode.TIEBREAK_7) }
    var goldenPoint by remember { mutableStateOf(false) }

    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Text(
                    text = "Nuevo partido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            item { SectionLabel("Deporte") }
            items(Sport.entries) { s ->
                SwitchButton(
                    checked = sport == s,
                    onCheckedChange = { if (it) sport = s },
                    label = { Text(s.displayName()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { SectionLabel("Sets a jugar") }
            items(listOf(1, 3, 5)) { n ->
                SwitchButton(
                    checked = bestOfSets == n,
                    onCheckedChange = { if (it) bestOfSets = n },
                    label = { Text("Al mejor de $n") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { SectionLabel("Set decisivo") }
            items(FinalSetMode.entries) { mode ->
                SwitchButton(
                    checked = finalSetMode == mode,
                    onCheckedChange = { if (it) finalSetMode = mode },
                    label = { Text(mode.displayName()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (sport == Sport.PADEL) {
                item { SectionLabel("Punto de oro") }
                item {
                    SwitchButton(
                        checked = goldenPoint,
                        onCheckedChange = { goldenPoint = it },
                        label = { Text(if (goldenPoint) "Activado" else "Desactivado") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            item {
                Button(
                    onClick = {
                        onStart(
                            MatchConfig(
                                sport = sport,
                                bestOfSets = bestOfSets,
                                finalSetMode = finalSetMode,
                                goldenPoint = sport == Sport.PADEL && goldenPoint,
                                playerNameA = "A",
                                playerNameB = "B",
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Empezar")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = TextAlign.Start,
    )
}

private fun Sport.displayName(): String = when (this) {
    Sport.TENNIS -> "Tenis"
    Sport.PADEL -> "Pádel"
}

private fun FinalSetMode.displayName(): String = when (this) {
    FinalSetMode.TIEBREAK_7 -> "Tie-break a 7"
    FinalSetMode.SUPER_TIEBREAK_10 -> "Super TB a 10"
    FinalSetMode.ADVANTAGE -> "Ventaja (sin TB)"
}
