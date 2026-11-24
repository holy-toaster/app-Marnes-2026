package com.gabriel.estante.ui.estante

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.gabriel.estante.data.local.EstanteDatabase
import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.repository.LivroRepository
import com.gabriel.estante.ui.theme.Ambar
import com.gabriel.estante.ui.theme.CapaPadrao
import com.gabriel.estante.ui.theme.Cartao
import com.gabriel.estante.ui.theme.TextoClaro
import com.gabriel.estante.ui.theme.TextoNoCartao
import com.gabriel.estante.ui.theme.TextoSecundarioNoCartao

/** Tela Estante (Home): cria o ViewModel (com a fábrica) e mostra o conteúdo. */
@Composable
fun EstanteScreen(aoAbrirDetalhe: (olId: String) -> Unit) {
    val contexto = LocalContext.current.applicationContext
    val viewModel: EstanteViewModel = viewModel(
        factory = EstanteViewModelFactory(
            LivroRepository(EstanteDatabase.getInstance(contexto).livroDao())
        )
    )
    val livros by viewModel.livros.collectAsState()
    val filtro by viewModel.filtro.collectAsState()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        // Título em fonte pesada, como no design
        Text(
            "Minha Estante",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )
        Chips(selecionado = filtro, aoSelecionar = viewModel::selecionarFiltro)

        if (livros.isEmpty()) {
            // Estado vazio
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Sua estante está vazia. Use a Pesquisa para adicionar livros.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(livros, key = { it.id }) { livro -> CartaoLivro(livro, aoClicar = { aoAbrirDetalhe(livro.olId) }) }
            }
        }
    }
}

// Linha de chips com rolagem horizontal; o selecionado fica branco
@Composable
private fun Chips(selecionado: FiltroEstante, aoSelecionar: (FiltroEstante) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroEstante.entries.forEach { filtro ->
            val ativo = filtro == selecionado
            Text(
                text = filtro.rotulo,
                color = if (ativo) TextoNoCartao else TextoClaro,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (ativo) Color.White else Color.Transparent)
                    .clickable { aoSelecionar(filtro) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// Cartão cinza-claro: capa, título, autor e barra de progresso com o percentual.
// Tocar no cartão abre a tela de Detalhes.
@Composable
private fun CartaoLivro(livro: Livro, aoClicar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Cartao)
            .clickable(onClick = aoClicar)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fundo colorido aparece enquanto a capa carrega ou se não houver capa
        Box(Modifier.width(55.dp).height(74.dp).background(CapaPadrao)) {
            if (livro.capaUrl != null) {
                AsyncImage(
                    model = livro.capaUrl,
                    contentDescription = "Capa de ${livro.titulo}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Barra âmbar; o progresso vai de 0 a 100, a barra espera de 0 a 1
                LinearProgressIndicator(
                    progress = { livro.progresso / 100f },
                    modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(50)),
                    color = Ambar,
                    trackColor = Color.White,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Text(
                    "${livro.progresso}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextoNoCartao,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}
