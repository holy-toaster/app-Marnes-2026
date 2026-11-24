package com.gabriel.estante.data.local

/**
 * Em qual "prateleira" o livro está.
 * O Room salva enums automaticamente no banco como texto (ex.: "LENDO").
 */
enum class StatusLeitura {
    QUERO_LER,
    LENDO,
    LIDO;

    companion object {
        /** O status segue o progresso: 0 = quero ler, de 1 a 99 = lendo, 100 = lido. */
        fun doProgresso(progresso: Int): StatusLeitura = when {
            progresso <= 0 -> QUERO_LER
            progresso >= 100 -> LIDO
            else -> LENDO
        }
    }
}
