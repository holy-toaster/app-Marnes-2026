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

    /** Busca um livro pelo id; devolve null se não existir. */
    @Query("SELECT * FROM livros WHERE id = :id")
    suspend fun buscarPorId(id: Long): Livro?
}
