package com.gabriel.estante.data.repository

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Um livro dos dados de exemplo (assets/livros_exemplo.json).
 * ATENÇÃO: as notas, as quantidades e as resenhas são FICTÍCIAS, feitas só para o app de estudo.
 */
@Serializable
data class NotaExemplo(
    val olId: String,
    val titulo: String,
    val autor: String,
    val capaId: Long,             // cover_i da Open Library
    val notaLeitores: Double,     // 0 a 5, uma casa decimal
    val qtdAvaliacoes: Int,
    val notaCriticos: Double,     // 0 a 5, uma casa decimal
    val resenha: String           // texto curto de exemplo
) {
    /** Endereço da capa na Open Library. */
    val capaUrl: String get() = "https://covers.openlibrary.org/b/id/$capaId-M.jpg"
}

/** Mostra a nota com uma casa decimal e vírgula, como no design (ex.: 4.6 -> "4,6"). */
fun Double.comoNota(): String = String.format(Locale("pt", "BR"), "%.1f", this)

/**
 * Lê os dados de exemplo do arquivo em assets. Não usa rede.
 * Nunca lança exceção: se o arquivo faltar ou estiver quebrado, devolve lista vazia.
 */
class NotasExemploRepository(private val contexto: Context) {

    // Lido só uma vez e guardado
    private var cache: List<NotaExemplo>? = null

    private val json = Json { ignoreUnknownKeys = true }

    /** Todos os livros de exemplo. */
    suspend fun listar(): List<NotaExemplo> {
        cache?.let { return it }
        val lista = withContext(Dispatchers.IO) {
            try {
                val texto = contexto.assets.open(ARQUIVO).bufferedReader().use { it.readText() }
                json.decodeFromString<List<NotaExemplo>>(texto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
        cache = lista
        return lista
    }

    /** Dados de exemplo de um livro pelo olId, ou null se ele não estiver no arquivo. */
    suspend fun buscarPorOlId(olId: String): NotaExemplo? = listar().firstOrNull { it.olId == olId }

    private companion object {
        const val ARQUIVO = "livros_exemplo.json"
    }
}
