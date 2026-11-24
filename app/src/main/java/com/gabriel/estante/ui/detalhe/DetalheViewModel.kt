package com.gabriel.estante.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.local.StatusLeitura
import com.gabriel.estante.data.remote.ResultadoSinopse
import com.gabriel.estante.data.repository.LivroRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estados da seção "Sinopse". */
sealed interface EstadoSinopse {
    data object Carregando : EstadoSinopse                  // mostra um indicador discreto
    data class Pronta(val texto: String) : EstadoSinopse
    data object Oculta : EstadoSinopse                      // sem sinopse ou erro: some da tela
}

/**
 * O que a tela sabe sobre o livro. "livro" é null quando ele veio da Pesquisa e
 * ainda não foi salvo; "carregado" é false só até o Room responder pela primeira vez.
 */
data class EstadoDetalhe(
    val carregado: Boolean = false,
    val livro: Livro? = null
)

class DetalheViewModel(
    private val repositorio: LivroRepository,
    private val olId: String,
    // Dados vindos da navegação: só servem para livros que ainda não estão no Room
    private val titulo: String,
    private val autor: String,
    private val capaUrl: String?
) : ViewModel() {

    // O livro salvo no Room (null = fora da estante). Como é um Flow, a tela se atualiza
    // sozinha quando adicionamos, mudamos o progresso, avaliamos ou removemos.
    val estado: StateFlow<EstadoDetalhe> = repositorio.observarPorOlId(olId)
        .map { EstadoDetalhe(carregado = true, livro = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDetalhe())

    private val _sinopse = MutableStateFlow<EstadoSinopse>(EstadoSinopse.Carregando)
    val sinopse: StateFlow<EstadoSinopse> = _sinopse

    init {
        carregarSinopse()
    }

    private fun carregarSinopse() {
        viewModelScope.launch {
            _sinopse.value = when (val r = repositorio.buscarSinopse(olId)) {
                is ResultadoSinopse.Sucesso -> EstadoSinopse.Pronta(r.texto)
                // Sem sinopse ou com erro (qualquer ErroBusca): esconde a seção, sem mensagem
                is ResultadoSinopse.Erro -> EstadoSinopse.Oculta
            }
        }
    }

    /** Salva o livro como QUERO_LER. O índice único do olId impede duplicar. */
    fun adicionar() {
        viewModelScope.launch {
            executarSemFalhar {
                repositorio.adicionar(
                    Livro(
                        titulo = titulo,
                        autor = autor,
                        capaUrl = capaUrl,
                        status = StatusLeitura.QUERO_LER,
                        olId = olId
                    )
                )
            }
        }
    }

    /** Salva o progresso (0 a 100); o repositório ajusta o status junto. */
    fun salvarProgresso(progresso: Int) {
        val livro = estado.value.livro ?: return
        viewModelScope.launch { executarSemFalhar { repositorio.atualizarProgresso(livro.id, progresso) } }
    }

    /** Salva a nota (1 a 5) e a anotação. */
    fun salvarAvaliacao(nota: Int, anotacao: String) {
        val livro = estado.value.livro ?: return
        viewModelScope.launch {
            executarSemFalhar { repositorio.atualizarNotaEAnotacao(livro.id, nota, anotacao.trim()) }
        }
    }

    /** Apaga o livro da estante (a tela já pediu confirmação). */
    fun remover() {
        val livro = estado.value.livro ?: return
        viewModelScope.launch { executarSemFalhar { repositorio.remover(livro) } }
    }

    // Uma falha no banco não deve derrubar o app; só deixamos de aplicar a ação
    private suspend fun executarSemFalhar(acao: suspend () -> Unit) {
        try {
            acao()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // ignorado de propósito
        }
    }
}

/** Fábrica simples (sem Hilt) que cria o DetalheViewModel com o repositório e os dados do livro. */
class DetalheViewModelFactory(
    private val repositorio: LivroRepository,
    private val olId: String,
    private val titulo: String,
    private val autor: String,
    private val capaUrl: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DetalheViewModel::class.java)) { "ViewModel desconhecido" }
        return DetalheViewModel(repositorio, olId, titulo, autor, capaUrl) as T
    }
}
