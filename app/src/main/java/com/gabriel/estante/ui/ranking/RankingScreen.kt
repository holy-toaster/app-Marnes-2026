package com.gabriel.estante.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.gabriel.estante.data.repository.NotaExemplo
import com.gabriel.estante.data.repository.NotasExemploRepository
import com.gabriel.estante.data.repository.comoNota
import com.gabriel.estante.ui.theme.Ambar
import com.gabriel.estante.ui.theme.CapaPadrao
import com.gabriel.estante.ui.theme.Cartao
import com.gabriel.estante.ui.theme.Fundo
import com.gabriel.estante.ui.theme.TextoClaro
import com.gabriel.estante.ui.theme.TextoNoCartao
import com.gabriel.estante.ui.theme.TextoSecundarioNoCartao

/** Tela Mais Avaliados: duas abas que ordenam os livros de exemplo pela nota. */
@Composable
fun RankingScreen(aoAbrirDetalhe: (NotaExemplo) -> Unit) {
    val contexto = LocalContext.current.applicationContext
    val viewModel: RankingViewModel = viewModel(
        factory = RankingViewModelFactory(NotasExemploRepository(contexto))
    )
    val livros by viewModel.livros.collectAsState()
    val aba by viewModel.aba.collectAsState()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Text(
            "Mais Avaliados",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )
        // Abas no mesmo estilo dos chips da Estante: a escolhida fica branca
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbaRanking.entries.forEach { item ->
                val ativa = item == aba
                Text(
                    text = item.rotulo,
                    color = if (ativa) TextoNoCartao else TextoClaro,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (ativa) Color.White else Color.Transparent)
                        .clickable { viewModel.selecionarAba(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Text(
            "Notas de exemplo",
            fontSize = 11.sp,
            color = TextoClaro.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(livros, key = { it.olId }) { livro ->
                CartaoRanking(livro, aoClicar = { aoAbrirDetalhe(livro) })
            }
        }
    }
}

// Cartão cinza-claro: capa, título, autor e os dois selos de nota
@Composable
private fun CartaoRanking(livro: NotaExemplo, aoClicar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Cartao)
            .clickable(onClick = aoClicar)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fundo vermelho aparece enquanto a capa carrega
        AsyncImage(
            model = livro.capaUrl,
            contentDescription = "Capa de ${livro.titulo}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 55.dp, height = 74.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CapaPadrao)
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                livro.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = TextoNoCartao,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                livro.autor,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = TextoSecundarioNoCartao,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Selo("${livro.notaLeitores.comoNota()} - Leitores", Ambar, TextoNoCartao)
                Selo("${livro.notaCriticos.comoNota()} - Críticos", Fundo, Color.White)
            }
        }
    }
}

// Selo arredondado "★ 4,6 - Leitores"
@Composable
private fun Selo(texto: String, fundo: Color, conteudo: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(fundo)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = conteudo, modifier = Modifier.size(11.dp))
        Text(" $texto", fontSize = 10.sp, color = conteudo, maxLines = 1)
    }
}
