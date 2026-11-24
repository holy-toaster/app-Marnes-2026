package com.gabriel.estante.data.repository

import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.local.LivroDao
import com.gabriel.estante.data.local.StatusLeitura
import com.gabriel.estante.data.remote.OpenLibraryApi
import com.gabriel.estante.data.remote.ResultadoBusca
import com.gabriel.estante.data.remote.ResultadoSinopse
import kotlinx.coroutines.flow.Flow

/**
 * Ponto único de acesso aos livros para os ViewModels.
 * Usa o banco local (Room) para a estante e a Open Library para buscar livros novos.
 */
class LivroRepository(
    private val dao: LivroDao,
    private val api: OpenLibraryApi = OpenLibraryApi()
) {

    /**
     * Busca livros pelo título na Open Library. Nunca lança exceção:
     * devolve ResultadoBusca.Sucesso ou ResultadoBusca.Erro.
     *
     * Exemplo de uso em um ViewModel:
     *
     *   class BuscaViewModel(private val repo: LivroRepository) : ViewModel() {
     *       var resultado by mutableStateOf<ResultadoBusca?>(null)
     *           private set
     *
     *       fun buscar(titulo: String) {
     *           viewModelScope.launch {                       // corrotina ligada ao ViewModel
     *               resultado = repo.buscarLivros(titulo)     // a tela só observa "resultado"
     *           }
     *       }
     *   }
     *
     *   // Na tela: when (val r = viewModel.resultado) {
     *   //     is ResultadoBusca.Sucesso -> mostra r.livros
     *   //     is ResultadoBusca.Erro    -> mostra mensagem conforme r.motivo
     *   //     null                      -> ainda não buscou
     *   // }
     */
    suspend fun buscarLivros(titulo: String): ResultadoBusca = api.buscarPorTitulo(titulo)

    /** Livros de uma prateleira, atualizados automaticamente. */
    fun livrosPorStatus(status: StatusLeitura): Flow<List<Livro>> =
        dao.listarPorStatus(status)

    /** Todos os livros da estante, de qualquer status, atualizados automaticamente. */
    fun todosOsLivros(): Flow<List<Livro>> = dao.listarTodos()

    suspend fun buscarPorId(id: Long): Livro? = dao.buscarPorId(id)

    /** Observa um livro pelo olId (null = não está na estante). */
    fun observarPorOlId(olId: String): Flow<Livro?> = dao.observarPorOlId(olId)

    /** Busca a sinopse da obra na Open Library. Nunca lança exceção. */
    suspend fun buscarSinopse(olId: String): ResultadoSinopse = api.buscarSinopse(olId)

    /** Atualiza o progresso (mantido entre 0 e 100) e ajusta o status de acordo com ele. */
    suspend fun atualizarProgresso(id: Long, progresso: Int) {
        val valido = progresso.coerceIn(0, 100)
        dao.atualizarProgresso(id, valido, StatusLeitura.doProgresso(valido))
    }

    /** Atualiza só a nota (0 a 5) e a anotação do livro. */
    suspend fun atualizarNotaEAnotacao(id: Long, nota: Int, anotacao: String) =
        dao.atualizarNotaEAnotacao(id, nota.coerceIn(0, 5), anotacao)

    /**
     * Salva um livro novo na estante.
     * Retorna true se salvou, ou false se ele já estava na estante (mesmo olId).
     */
    suspend fun adicionar(livro: Livro): Boolean =
        dao.inserir(livro.comNotaValida()) != -1L

    suspend fun atualizar(livro: Livro) = dao.atualizar(livro.comNotaValida())

    suspend fun remover(livro: Livro) = dao.remover(livro)

    // Garante que a nota fique entre 0 e 5 e o progresso entre 0 e 100
    private fun Livro.comNotaValida(): Livro =
        copy(notaPessoal = notaPessoal.coerceIn(0, 5), progresso = progresso.coerceIn(0, 100))
}
