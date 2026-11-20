package com.gabriel.estante.data.local

/**
 * Em qual "prateleira" o livro está.
 * O Room salva enums automaticamente no banco como texto (ex.: "LENDO").
 */
enum class StatusLeitura {
    QUERO_LER,
    LENDO,
    LIDO
}
