package com.gabriel.estante.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Ícone de livro (o pacote de ícones básico do Material não tem um). */
val IconeLivro: ImageVector = ImageVector.Builder(
    name = "Livro",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        // Capa do livro com um marcador na parte de cima
        moveTo(6f, 2f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        verticalLineTo(20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        horizontalLineTo(18f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        verticalLineTo(4f)
        curveTo(20f, 2.9f, 19.1f, 2f, 18f, 2f)
        close()
        moveTo(11f, 4f)
        horizontalLineTo(13f)
        verticalLineTo(9f)
        lineTo(12f, 8.2f)
        lineTo(11f, 9f)
        close()
    }
}.build()
