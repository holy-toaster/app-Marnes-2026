package com.gabriel.estante.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// O app tem só o tema escuro, sem cores dinâmicas do sistema.
private val EsquemaEscuro = darkColorScheme(
    primary = Ambar,
    onPrimary = TextoNoCartao,
    background = Fundo,
    onBackground = TextoClaro,
    surface = Fundo,
    onSurface = TextoClaro,
    surfaceVariant = Cartao,          // cartões
    onSurfaceVariant = TextoNoCartao  // texto dos cartões
)

@Composable
fun EstanteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaEscuro,
        typography = Typography,
        content = content
    )
}
