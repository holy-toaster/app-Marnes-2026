package com.gabriel.estante.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gabriel.estante.data.repository.NotaExemplo
import com.gabriel.estante.data.repository.NotasExemploRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** As duas abas da tela. */
enum class AbaRanking(val rotulo: String) {
    LEITORES("Pelos Leitores"),
    CRITICOS("Pelos Críticos")
}

class RankingViewModel(repositorio: NotasExemploRepository) : ViewModel() {

    private val _aba = MutableStateFlow(AbaRanking.LEITORES)
    val aba: StateFlow<AbaRanking> = _aba

    private val todos = MutableStateFlow<List<NotaExemplo>>(emptyList())

    // Lista ordenada da maior nota para a menor, conforme a aba escolhida
    val livros: StateFlow<List<NotaExemplo>> = combine(todos, _aba) { lista, aba ->
        when (aba) {
            AbaRanking.LEITORES -> lista.sortedByDescending { it.notaLeitores }
            AbaRanking.CRITICOS -> lista.sortedByDescending { it.notaCriticos }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { todos.value = repositorio.listar() }
    }

    fun selecionarAba(nova: AbaRanking) {
        _aba.value = nova
    }
}

/** Fábrica simples (sem Hilt) que cria o RankingViewModel. */
class RankingViewModelFactory(private val repositorio: NotasExemploRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RankingViewModel::class.java)) { "ViewModel desconhecido" }
        return RankingViewModel(repositorio) as T
    }
}
