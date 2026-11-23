package com.gabriel.estante.data.remote

/** Um livro encontrado na Open Library, já no formato que a tela usa. */
data class LivroBusca(
    val olId: String,        // ex.: "OL45804W" (identifica o livro na Open Library)
    val titulo: String,
    val autor: String,       // vazio se a API não informar autor
    val capaUrl: String?     // null se o livro não tem capa
)

/** Motivos pelos quais uma busca pode falhar. A tela escolhe a mensagem para cada um. */
enum class ErroBusca {
    SEM_INTERNET,
    TIMEOUT,
    RESPOSTA_VAZIA,   // a busca funcionou, mas não achou nenhum livro
    ERRO_SERVIDOR     // servidor com problema ou resposta que não deu para ler
}

/** Resultado de uma busca: ou deu certo (com a lista), ou deu erro (com o motivo). */
sealed interface ResultadoBusca {
    data class Sucesso(val livros: List<LivroBusca>) : ResultadoBusca
    data class Erro(val motivo: ErroBusca) : ResultadoBusca
}
