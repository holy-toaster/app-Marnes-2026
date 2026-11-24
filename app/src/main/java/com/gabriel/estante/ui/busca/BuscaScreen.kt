package com.gabriel.estante.ui.busca

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.gabriel.estante.data.local.EstanteDatabase
import com.gabriel.estante.data.remote.ErroBusca
import com.gabriel.estante.data.remote.LivroBusca
import com.gabriel.estante.data.repository.LivroRepository
import com.gabriel.estante.ui.theme.Ambar
import com.gabriel.estante.ui.theme.CapaPadrao
import com.gabriel.estante.ui.theme.TextoNoCartao
import com.gabriel.estante.ui.theme.TextoSecundarioNoCartao

/** Tela de Pesquisa: cria o ViewModel (com a fábrica) e mostra o conteúdo. */
@Composable
fun BuscaScreen() {
    val contexto = LocalContext.current.applicationContext
    val viewModel: BuscaViewModel = viewModel(
        factory = BuscaViewModelFactory(
            LivroRepository(EstanteDatabase.getInstance(contexto).livroDao())
        )
    )
    val estado by viewModel.estado.collectAsState()
    val consulta by viewModel.consulta.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostra no Snackbar cada mensagem enviada pelo ViewModel
    LaunchedEffect(viewModel) {
        viewModel.mensagens.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            CampoBusca(
                texto = consulta,
                aoMudar = viewModel::aoMudarConsulta,
                aoConfirmar = viewModel::buscar
            )
            Box(Modifier.fillMaxSize().padding(top = 16.dp)) {
                when (val e = estado) {
                    EstadoBusca.Inicial -> Mensagem("Digite o título de um livro e toque em buscar.")
                    EstadoBusca.Carregando ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = Ambar)
                    EstadoBusca.SemResultados -> Mensagem("Nenhum livro encontrado. Tente outro título.")
                    is EstadoBusca.Erro -> Erro(e.motivo, aoTentarDeNovo = viewModel::buscar)
                    is EstadoBusca.Resultados -> ListaResultados(e.livros, viewModel::adicionar)
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

// Campo arredondado com lupa; a busca roda ao tocar no botão de busca do teclado
@Composable
private fun CampoBusca(texto: String, aoMudar: (String) -> Unit, aoConfirmar: () -> Unit) {
    TextField(
        value = texto,
        onValueChange = aoMudar,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        placeholder = { Text("Pesquisar livros") },
        singleLine = true,
        shape = RoundedCornerShape(50),
        trailingIcon = { Icon(Icons.Filled.Search, contentDescription = "Pesquisar") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { aoConfirmar() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = TextoNoCartao,
            unfocusedTextColor = TextoNoCartao,
            focusedPlaceholderColor = TextoSecundarioNoCartao,
            unfocusedPlaceholderColor = TextoSecundarioNoCartao,
            focusedTrailingIconColor = TextoSecundarioNoCartao,
            unfocusedTrailingIconColor = TextoSecundarioNoCartao,
            cursorColor = TextoNoCartao,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun Mensagem(texto: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(texto, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

// Erro com mensagem em português e botão para repetir a busca
@Composable
private fun Erro(motivo: ErroBusca, aoTentarDeNovo: () -> Unit) {
    val texto = when (motivo) {
        ErroBusca.SEM_INTERNET -> "Sem conexão com a internet. Verifique sua rede e tente de novo."
        ErroBusca.TIMEOUT -> "A busca demorou demais para responder. Tente de novo."
        else -> "Não foi possível buscar os livros agora. Tente de novo em instantes."
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(texto, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = aoTentarDeNovo,
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ambar, contentColor = TextoNoCartao)
        ) { Text("Tentar de novo") }
    }
}

@Composable
private fun ListaResultados(livros: List<LivroBusca>, aoAdicionar: (LivroBusca) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(livros, key = { it.olId }) { livro ->
            CartaoLivro(livro, aoAdicionar = { aoAdicionar(livro) })
        }
    }
}

// Cartão cinza-claro: capa, título, autor e botão "Adicionar" (tocar no cartão ainda não abre nada)
@Composable
private fun CartaoLivro(livro: LivroBusca, aoAdicionar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fundo vermelho aparece enquanto a capa carrega ou se o livro não tem capa
        AsyncImage(
            model = livro.capaUrl,
            contentDescription = "Capa de ${livro.titulo}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 55.dp, height = 80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CapaPadrao)
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                livro.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = TextoNoCartao,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                livro.autor.ifEmpty { "Autor desconhecido" },
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundarioNoCartao,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = aoAdicionar,
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ambar, contentColor = TextoNoCartao)
        ) { Text("Adicionar") }
    }
}
