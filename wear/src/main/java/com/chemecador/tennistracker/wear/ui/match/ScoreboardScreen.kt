package com.chemecador.tennistracker.wear.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.chemecador.tennistracker.scoring.GamePhase
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.Side

@Composable
fun ScoreboardScreen(viewModel: MatchSessionViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return

    KeepScreenOn()

    var showEndDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SideZone(
            side = Side.A,
            state = current,
            onTap = { viewModel.onPoint(Side.A) },
            background = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        CenterStrip(
            state = current,
            onUndo = viewModel::onUndo,
            onEndMatch = { showEndDialog = true },
        )
        SideZone(
            side = Side.B,
            state = current,
            onTap = { viewModel.onPoint(Side.B) },
            background = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }

    AlertDialog(
        visible = showEndDialog && current.winner == null,
        onDismissRequest = { showEndDialog = false },
        title = { Text("¿Terminar partido?", textAlign = TextAlign.Center) },
        text = { Text("Elige el ganador", textAlign = TextAlign.Center) },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.endMatchEarly(Side.A)
                    showEndDialog = false
                },
            ) { Text(current.config.playerNameA) }
        },
        dismissButton = {
            Button(
                onClick = {
                    viewModel.endMatchEarly(Side.B)
                    showEndDialog = false
                },
            ) { Text(current.config.playerNameB) }
        },
    )
}

@Composable
private fun SideZone(
    side: Side,
    state: MatchState,
    onTap: () -> Unit,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val name = if (side == Side.A) state.config.playerNameA else state.config.playerNameB
    val isServing = state.server == side
    val pointsLabel = pointsLabel(state.phase, side)

    Box(
        modifier = modifier
            .background(background)
            .clickable { onTap() }
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isServing) "• $name" else name,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = pointsLabel,
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun CenterStrip(
    state: MatchState,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
) {
    val canUndo = state.history.isNotEmpty()
    val undoColor = if (canUndo) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val canEnd = state.winner == null
    val endColor = if (canEnd) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "↶",
            color = undoColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(enabled = canUndo) { onUndo() },
        )
        Text(
            text = setsLine(state),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = "⏹",
            color = endColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(enabled = canEnd) { onEndMatch() },
        )
    }
}

private fun pointsLabel(phase: GamePhase, side: Side): String = when (phase) {
    is GamePhase.Normal -> tennisPoints(if (side == Side.A) phase.score.a else phase.score.b)
    GamePhase.Deuce -> "40"
    is GamePhase.Advantage -> if (phase.side == side) "Ad" else "—"
    is GamePhase.TieBreak -> if (side == Side.A) phase.score.a.toString() else phase.score.b.toString()
}

private fun tennisPoints(p: Int): String = when (p) {
    0 -> "0"
    1 -> "15"
    2 -> "30"
    3 -> "40"
    else -> p.toString()
}

private fun setsLine(state: MatchState): String {
    val parts = state.completedSets.map { set ->
        val tb = set.tieBreak
        if (tb != null && set.gamesA + set.gamesB <= 1) {
            // Super-TB representation: show TB score directly
            "${tb.a}-${tb.b}"
        } else if (tb != null) {
            "${set.gamesA}-${set.gamesB}(${minOf(tb.a, tb.b)})"
        } else {
            "${set.gamesA}-${set.gamesB}"
        }
    } + run {
        val (a, b) = state.currentSetGames
        if (state.winner == null) listOf("$a-$b") else emptyList()
    }
    return parts.joinToString("  ")
}

