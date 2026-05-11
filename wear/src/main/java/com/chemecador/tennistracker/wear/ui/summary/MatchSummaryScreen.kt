package com.chemecador.tennistracker.wear.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.SetScore
import com.chemecador.tennistracker.scoring.Side

@Composable
fun MatchSummaryScreen(state: MatchState?, onNewMatch: () -> Unit) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state == null) {
                item { Text("Sin partido") }
                return@ScalingLazyColumn
            }

            val winnerName = when (state.winner) {
                Side.A -> state.config.playerNameA
                Side.B -> state.config.playerNameB
                null -> "—"
            }

            item {
                Text(
                    text = "Ganador",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    text = winnerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            item { Spacer(Modifier.height(2.dp)) }

            item {
                Text(
                    text = "Sets",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }

            state.completedSets.forEachIndexed { idx, set ->
                item {
                    Text(
                        text = "${idx + 1}.  ${formatSet(set)}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            item {
                Button(
                    onClick = onNewMatch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Nuevo partido")
                }
            }
        }
    }
}

private fun formatSet(set: SetScore): String {
    val tb = set.tieBreak
    return when {
        tb != null && set.gamesA + set.gamesB <= 1 -> "${tb.a}-${tb.b}  (super TB)"
        tb != null -> "${set.gamesA}-${set.gamesB}  (${tb.a}-${tb.b})"
        else -> "${set.gamesA}-${set.gamesB}"
    }
}
