package com.gabriel.estante.data.repository

import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.local.LivroDao
import com.gabriel.estante.data.local.StatusLeitura
import kotlinx.coroutines.flow.Flow

/**
 * Ponto único de acesso aos livros para os ViewModels.
 * Hoje só usa o banco local; depois também vai usar a Open Library.
 */
class LivroRepository(private val dao: LivroDao) {

    /** Livros de uma prateleira, atualizados automaticamente. */
    fun livrosPorStatus(status: StatusLeitura): Flow<List<Livro>> =
        dao.listarPorStatus(status)

    suspend fun buscarPorId(id: Long): Livro? = dao.buscarPorId(id)

    /**
     * Salva um livro novo na estante.
     * Retorna true se salvou, ou false se ele já estava na estante (mesmo olId).
     */
    suspend fun adicionar(livro: Livro): Boolean =
        dao.inserir(livro.comNotaValida()) != -1L

    suspend fun atualizar(livro: Livro) = dao.atualizar(livro.comNotaValida())

    suspend fun remover(livro: Livro) = dao.remover(livro)

    // Garante que a nota fique sempre entre 0 e 5
    private fun Livro.comNotaValida(): Livro = copy(notaPessoal = notaPessoal.coerceIn(0, 5))
}
