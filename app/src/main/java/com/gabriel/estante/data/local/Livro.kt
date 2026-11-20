package com.gabriel.estante.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Um livro salvo na estante. Cada objeto vira uma linha da tabela "livros".
 *
 * O índice único em "olId" impede que o mesmo livro da Open Library
 * seja salvo duas vezes.
 */
@Entity(
    tableName = "livros",
    indices = [Index(value = ["olId"], unique = true)]
)
data class Livro(
    // Gerado pelo banco: deixe 0 ao criar um livro novo
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val autor: String,
    // Endereço da imagem da capa; null quando o livro não tem capa
    val capaUrl: String? = null,
    val status: StatusLeitura = StatusLeitura.QUERO_LER,
    // Nota de 0 a 5 (0 = sem nota). O repositório garante esse limite.
    val notaPessoal: Int = 0,
    // Texto livre escrito pelo usuário
    val anotacao: String = "",
    // Identificador do livro na Open Library (ex.: "/works/OL45804W")
    val olId: String
)
