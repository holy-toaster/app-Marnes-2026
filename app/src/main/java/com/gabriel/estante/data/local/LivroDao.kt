package com.gabriel.estante.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): as operações que o app faz na tabela "livros".
 * O Room gera a implementação destas funções automaticamente (via KSP).
 */
@Dao
interface LivroDao {

    /**
     * Insere um livro e devolve o id gerado.
     * Se já existir um livro com o mesmo olId, nada é inserido e o retorno é -1.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(livro: Livro): Long

    /** Atualiza um livro já salvo (procurado pelo id). */
    @Update
    suspend fun atualizar(livro: Livro)

    /** Remove um livro da estante. */
    @Delete
    suspend fun remover(livro: Livro)

    /**
     * Lista os livros de uma prateleira, em ordem alfabética.
     * Como retorna Flow, a tela é avisada sozinha sempre que os dados mudam.
     */
    @Query("SELECT * FROM livros WHERE status = :status ORDER BY titulo COLLATE NOCASE")
    fun listarPorStatus(status: StatusLeitura): Flow<List<Livro>>

    /** Lista todos os livros da estante (de qualquer status), em ordem alfabética. */
    @Query("SELECT * FROM livros ORDER BY titulo COLLATE NOCASE")
    fun listarTodos(): Flow<List<Livro>>

    /** Atualiza só o progresso (0 a 100) de um livro, sem mexer nos outros campos. */
    @Query("UPDATE livros SET progresso = :progresso WHERE id = :id")
    suspend fun atualizarProgresso(id: Long, progresso: Int)

    /** Atualiza só a nota e a anotação de um livro, sem mexer nos outros campos. */
    @Query("UPDATE livros SET notaPessoal = :nota, anotacao = :anotacao WHERE id = :id")
    suspend fun atualizarNotaEAnotacao(id: Long, nota: Int, anotacao: String)

    /** Busca um livro pelo id; devolve null se não existir. */
    @Query("SELECT * FROM livros WHERE id = :id")
    suspend fun buscarPorId(id: Long): Livro?
}
