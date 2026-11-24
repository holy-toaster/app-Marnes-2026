package com.gabriel.estante.ui.busca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.local.StatusLeitura
import com.gabriel.estante.data.remote.ErroBusca
import com.gabriel.estante.data.remote.LivroBusca
import com.gabriel.estante.data.remote.ResultadoBusca
import com.gabriel.estante.data.repository.LivroRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Os estados possíveis da tela de Pesquisa. */
sealed interface EstadoBusca {
    data object Inicial : EstadoBusca                       // ainda não pesquisou
    data object Carregando : EstadoBusca
    data class Resultados(val livros: List<LivroBusca>) : EstadoBusca
    data object SemResultados : EstadoBusca
    data class Erro(val motivo: ErroBusca) : EstadoBusca    // falha de rede, timeout ou servidor
}

class BuscaViewModel(private val repositorio: LivroRepository) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoBusca>(EstadoBusca.Inicial)
    val estado: StateFlow<EstadoBusca> = _estado

    // Texto digitado no campo (fica aqui para sobreviver à rotação da tela)
    private val _consulta = MutableStateFlow("")
    val consulta: StateFlow<String> = _consulta

    // Mensagens de uso único para o Snackbar
    private val _mensagens = Channel<String>(Channel.BUFFERED)
    val mensagens = _mensagens.receiveAsFlow()

    fun aoMudarConsulta(texto: String) {
        _consulta.value = texto
    }

    /** Pesquisa o texto atual (ao confirmar no teclado ou em "Tentar de novo"). */
    fun buscar() {
        val texto = _consulta.value.trim()
        if (texto.isEmpty()) return

        _estado.value = EstadoBusca.Carregando
        viewModelScope.launch {
            _estado.value = when (val r = repositorio.buscarLivros(texto)) {
                is ResultadoBusca.Sucesso -> EstadoBusca.Resultados(r.livros)
                is ResultadoBusca.Erro ->
                    if (r.motivo == ErroBusca.RESPOSTA_VAZIA) EstadoBusca.SemResultados
                    else EstadoBusca.Erro(r.motivo)
            }
        }
    }

    /** Salva o livro na estante como "Quero ler", sem duplicar (o olId é único no banco). */
    fun adicionar(livro: LivroBusca) {
        viewModelScope.launch {
            val mensagem = try {
                val salvou = repositorio.adicionar(
                    Livro(
                        titulo = livro.titulo,
                        autor = livro.autor,
                        capaUrl = livro.capaUrl,
                        status = StatusLeitura.QUERO_LER,
                        olId = livro.olId
                    )
                )
                if (salvou) "Livro adicionado à estante" else "Este livro já está na sua estante"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Não foi possível adicionar o livro. Tente de novo."
            }
            _mensagens.send(mensagem)
        }
    }
}

/** Fábrica simples (sem Hilt) que cria o BuscaViewModel com o repositório. */
class BuscaViewModelFactory(private val repositorio: LivroRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(BuscaViewModel::class.java)) { "ViewModel desconhecido" }
        return BuscaViewModel(repositorio) as T
    }
}
