package com.gabriel.estante.ui.estante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.local.StatusLeitura
import com.gabriel.estante.data.repository.LivroRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Os chips do topo da tela. "status" nulo significa "mostrar todos". */
enum class FiltroEstante(val rotulo: String, val status: StatusLeitura?) {
    TODOS("Todos", null),
    QUERO_LER("Quero ler", StatusLeitura.QUERO_LER),
    LENDO("Lendo", StatusLeitura.LENDO),
    LIDOS("Lidos", StatusLeitura.LIDO)
}

class EstanteViewModel(repositorio: LivroRepository) : ViewModel() {

    // Chip selecionado no momento
    private val _filtro = MutableStateFlow(FiltroEstante.TODOS)
    val filtro: StateFlow<FiltroEstante> = _filtro

    // Livros do filtro atual, lidos do Room como Flow: a lista se atualiza sozinha
    // quando o banco muda ou quando o usuário troca de chip.
    @OptIn(ExperimentalCoroutinesApi::class)
    val livros: StateFlow<List<Livro>> = _filtro
        .flatMapLatest { f ->
            val status = f.status
            if (status == null) repositorio.todosOsLivros()
            else repositorio.livrosPorStatus(status)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selecionarFiltro(novo: FiltroEstante) {
        _filtro.value = novo
    }
}

/** Fábrica simples (sem Hilt) que cria o EstanteViewModel com o repositório. */
class EstanteViewModelFactory(private val repositorio: LivroRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(EstanteViewModel::class.java)) { "ViewModel desconhecido" }
        return EstanteViewModel(repositorio) as T
    }
}
