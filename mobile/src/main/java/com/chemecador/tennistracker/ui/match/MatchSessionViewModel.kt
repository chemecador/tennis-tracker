package com.chemecador.tennistracker.ui.match

import androidx.lifecycle.ViewModel
import com.chemecador.tennistracker.scoring.MatchConfig
import com.chemecador.tennistracker.scoring.MatchState
import com.chemecador.tennistracker.scoring.Scorer
import com.chemecador.tennistracker.scoring.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MatchSessionViewModel : ViewModel() {

    private val _state = MutableStateFlow<MatchState?>(null)
    val state = _state.asStateFlow()

    fun start(config: MatchConfig) {
        _state.value = Scorer.newMatch(config)
    }

    fun onPoint(side: Side) {
        _state.update { current -> current?.let { Scorer.winPoint(it, side) } }
    }

    fun onUndo() {
        _state.update { current -> current?.let(Scorer::undo) }
    }

    fun reset() {
        _state.value = null
    }
}
