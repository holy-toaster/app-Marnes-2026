package com.gabriel.estante.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// ---------------------------------------------------------------------------
// Modelos do JSON da Open Library (só os campos que pedimos).
// Todos têm valor padrão, porque a API nem sempre manda todos os campos.
// ---------------------------------------------------------------------------

@Serializable
private data class RespostaBusca(
    val docs: List<DocLivro> = emptyList()
)

@Serializable
private data class DocLivro(
    val key: String = "",                                   // ex.: "/works/OL45804W"
    val title: String = "",
    @SerialName("author_name") val autores: List<String> = emptyList(),
    @SerialName("cover_i") val capaId: Long? = null         // id da capa (pode não existir)
)

// A sinopse ("description") pode vir como texto simples ("Um texto...")
// ou como objeto ({"type": "/type/text", "value": "Um texto..."}).
// Por isso lemos como JsonElement e tratamos os dois casos na função.
@Serializable
private data class RespostaObra(
    val description: JsonElement? = null
)

// Linha que é só um separador ("--", "---", "----"...)
private val SEPARADOR = Regex("""(?m)^[ 	]*-{2,}[ 	]*$""")
// Link em Markdown: [texto](endereço)
private val LINK_MARKDOWN = Regex("""\[([^\]]*)\]\([^)]*\)""")

/**
 * Limpa a sinopse da Open Library antes de mostrar na tela.
 *
 * Antes (texto cru da API):
 *   Um faroleiro encontra um diário. Leia [mais](https://exemplo.com/farol).
 *
 *   ----------
 *   See also:
 *   * [Outro livro](https://exemplo.com/outro)
 * Depois:
 *   Um faroleiro encontra um diário. Leia mais.
 *
 * Passos: 1) corta tudo a partir de um separador, de "See also:" ou de "([source]";
 * 2) troca [texto](endereço) só por "texto"; 3) tira espaços e linhas em branco das pontas.
 * Se sobrar texto vazio, quem chama deve esconder a seção.
 */
fun limparSinopse(texto: String): String {
    // 1) Acha o ponto de corte mais cedo entre os marcadores de rodapé
    val corte = listOfNotNull(
        SEPARADOR.find(texto)?.range?.first,
        texto.indexOf("See also:").takeIf { it >= 0 },
        texto.indexOf("([source]").takeIf { it >= 0 }
    ).minOrNull()
    val semRodape = if (corte != null) texto.substring(0, corte) else texto

    // 2) [texto](endereço) -> texto   e   3) tira as sobras das pontas
    return semRodape.replace(LINK_MARKDOWN, "$1").trim()
}

/**
 * Acesso à API de busca da Open Library.
 * Nenhuma função daqui lança exceção: sempre devolve um [ResultadoBusca].
 */
class OpenLibraryApi(
    private val client: HttpClient = criarClient()
) {

    /**
     * Busca livros pelo título.
     * É uma função suspend: chame dentro de uma corrotina (ex.: viewModelScope.launch).
     */
    suspend fun buscarPorTitulo(titulo: String): ResultadoBusca {
        val consulta = titulo.trim()
        if (consulta.isEmpty()) return ResultadoBusca.Erro(ErroBusca.RESPOSTA_VAZIA)

        return try {
            val resposta = client.get(URL_BUSCA) {
                parameter("title", consulta)
                parameter("fields", "key,title,author_name,cover_i") // só o necessário
                parameter("limit", LIMITE_RESULTADOS)
            }

            if (!resposta.status.isSuccess()) {
                return ResultadoBusca.Erro(ErroBusca.ERRO_SERVIDOR)
            }

            val livros = resposta.body<RespostaBusca>().docs
                .filter { it.key.isNotBlank() && it.title.isNotBlank() } // descarta itens incompletos
                .map { it.paraLivroBusca() }

            if (livros.isEmpty()) {
                ResultadoBusca.Erro(ErroBusca.RESPOSTA_VAZIA)
            } else {
                ResultadoBusca.Sucesso(livros)
            }
        } catch (e: CancellationException) {
            throw e // cancelar a corrotina é normal (ex.: usuário saiu da tela): não pode ser engolido
        } catch (e: Exception) {
            ResultadoBusca.Erro(e.paraErroBusca())
        }
    }

    /**
     * Busca a sinopse da obra (endpoint /works/{id}.json), com o mesmo tratamento de erros da busca.
     * Se a obra não tem sinopse, devolve Erro(RESPOSTA_VAZIA).
     */
    suspend fun buscarSinopse(olId: String): ResultadoSinopse {
        if (olId.isBlank()) return ResultadoSinopse.Erro(ErroBusca.RESPOSTA_VAZIA)

        return try {
            val resposta = client.get("$URL_OBRAS/${olId.substringAfterLast('/')}.json")

            if (!resposta.status.isSuccess()) {
                return ResultadoSinopse.Erro(ErroBusca.ERRO_SERVIDOR)
            }

            val descricao = resposta.body<RespostaObra>().description
            val texto = when (descricao) {
                is JsonPrimitive -> descricao.contentOrNull                       // texto simples
                is JsonObject -> descricao["value"]?.jsonPrimitive?.contentOrNull // objeto com "value"
                else -> null
            }?.let { limparSinopse(it) } // tira Markdown e rodapés de fontes

            if (texto.isNullOrEmpty()) {
                ResultadoSinopse.Erro(ErroBusca.RESPOSTA_VAZIA)
            } else {
                ResultadoSinopse.Sucesso(texto)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoSinopse.Erro(e.paraErroBusca())
        }
    }

    // Transforma o item do JSON no objeto simples que a tela usa
    private fun DocLivro.paraLivroBusca() = LivroBusca(
        olId = key.substringAfterLast('/'),           // "/works/OL45804W" -> "OL45804W"
        titulo = title,
        autor = autores.firstOrNull().orEmpty(),      // usa o primeiro autor; vazio se não houver
        capaUrl = capaId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
    )

    // Descobre o motivo do erro olhando o tipo da exceção
    private fun Exception.paraErroBusca(): ErroBusca = when (this) {
        is HttpRequestTimeoutException,
        is SocketTimeoutException -> ErroBusca.TIMEOUT       // (inclui o timeout de conexão do Ktor)
        is UnresolvedAddressException,
        is IOException -> {
            // Timeout de conexão do Ktor também é IOException; separamos pelo nome da classe
            if (this::class.simpleName?.contains("Timeout") == true) ErroBusca.TIMEOUT
            else ErroBusca.SEM_INTERNET
        }
        else -> ErroBusca.ERRO_SERVIDOR                      // JSON inesperado, etc.
    }

    companion object {
        private const val URL_BUSCA = "https://openlibrary.org/search.json"
        private const val URL_OBRAS = "https://openlibrary.org/works"
        private const val LIMITE_RESULTADOS = 20

        // A Open Library pede um User-Agent que identifique o app e um contato.

        private const val EMAIL_CONTATO = "gabrieljbotelhosilva2431@gmail.com"
        private const val USER_AGENT = "Estante/1.0 (app Android de estudo; $EMAIL_CONTATO)"

        /** Cria o cliente Ktor: JSON, timeouts e User-Agent. */
        fun criarClient(): HttpClient = HttpClient(Android) {
            // Lê o JSON e ignora campos que não existem nas nossas classes
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            // Evita que a tela fique esperando para sempre
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 20_000
            }
            install(UserAgent) { agent = USER_AGENT }
        }
    }
}
