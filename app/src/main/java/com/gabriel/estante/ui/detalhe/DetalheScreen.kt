package com.gabriel.estante.ui.detalhe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.gabriel.estante.data.local.EstanteDatabase
import com.gabriel.estante.data.local.Livro
import com.gabriel.estante.data.local.StatusLeitura
import com.gabriel.estante.data.repository.LivroRepository
import com.gabriel.estante.ui.theme.Ambar
import com.gabriel.estante.ui.theme.CapaPadrao
import com.gabriel.estante.ui.theme.Cartao
import com.gabriel.estante.ui.theme.TextoNoCartao
import com.gabriel.estante.ui.theme.TextoSecundarioNoCartao

/**
 * Tela de Detalhes. Tem dois estados, decididos pelo Room:
 * - livro fora da estante: botão "Adicionar à Estante";
 * - livro na estante: "Avaliar", progresso, nota/anotação e "Remover da estante".
 *
 * [titulo], [autor] e [capaUrl] só são usados se o livro ainda não estiver no Room.
 */
@Composable
fun DetalheScreen(
    olId: String,
    titulo: String,
    autor: String,
    capaUrl: String?,
    aoVoltar: () -> Unit
) {
    val contexto = LocalContext.current.applicationContext
    val viewModel: DetalheViewModel = viewModel(
        key = olId,
        factory = DetalheViewModelFactory(
            LivroRepository(EstanteDatabase.getInstance(contexto).livroDao()),
            olId, titulo, autor, capaUrl
        )
    )
    val estado by viewModel.estado.collectAsState()
    val sinopse by viewModel.sinopse.collectAsState()
    val livro = estado.livro

    var mostrarAvaliacao by remember { mutableStateOf(false) }
    var mostrarRemocao by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Dados do Room têm prioridade; senão, usa o que veio pela navegação
        Topo(
            titulo = livro?.titulo ?: titulo,
            autor = livro?.autor ?: autor,
            capaUrl = livro?.capaUrl ?: capaUrl,
            aoVoltar = aoVoltar
        ) {
            // Só mostra o botão depois que o Room respondeu, para não piscar o estado errado
            if (estado.carregado) {
                if (livro == null) {
                    BotaoAmbar("Adicionar à Estante", viewModel::adicionar)
                } else {
                    BotaoAmbar("Avaliar", { mostrarAvaliacao = true }, comEstrela = true)
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            SecaoSinopse(sinopse)

            if (livro != null) {
                Progresso(livro, aoSalvar = viewModel::salvarProgresso)
                Avaliacao(livro)
                TextButton(
                    onClick = { mostrarRemocao = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                ) { Text("Remover da estante", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (mostrarAvaliacao && livro != null) {
        DialogoAvaliacao(
            notaInicial = livro.notaPessoal,
            anotacaoInicial = livro.anotacao,
            aoSalvar = { nota, anotacao ->
                viewModel.salvarAvaliacao(nota, anotacao)
                mostrarAvaliacao = false
            },
            aoCancelar = { mostrarAvaliacao = false }
        )
    }

    if (mostrarRemocao && livro != null) {
        AlertDialog(
            onDismissRequest = { mostrarRemocao = false },
            title = { Text("Remover da estante?") },
            text = { Text("\"${livro.titulo}\" será apagado da sua estante, junto com progresso, nota e anotação.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarRemocao = false
                    viewModel.remover()
                    aoVoltar()
                }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { mostrarRemocao = false }) { Text("Cancelar") } }
        )
    }
}

// Topo cinza-claro com cantos inferiores arredondados: seta, capa, título, autor e o botão principal
@Composable
private fun Topo(
    titulo: String,
    autor: String,
    capaUrl: String?,
    aoVoltar: () -> Unit,
    botao: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Cartao)
            .statusBarsPadding()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextoNoCartao)
            }
        }
        // Fundo vermelho aparece enquanto a capa carrega ou se não houver capa
        AsyncImage(
            model = capaUrl,
            contentDescription = "Capa de $titulo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 170.dp, height = 240.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CapaPadrao)
        )
        Text(
            titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = TextoNoCartao,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)
        )
        Text(
            autor.ifEmpty { "Autor desconhecido" },
            style = MaterialTheme.typography.titleMedium,
            color = TextoSecundarioNoCartao,
            textAlign = TextAlign.Center
        )
        Box(Modifier.padding(top = 16.dp)) { botao() }
    }
}

@Composable
private fun BotaoAmbar(texto: String, aoClicar: () -> Unit, comEstrela: Boolean = false) {
    Button(
        onClick = aoClicar,
        colors = ButtonDefaults.buttonColors(containerColor = Ambar, contentColor = TextoNoCartao)
    ) {
        if (comEstrela) Icon(Icons.Filled.Star, contentDescription = null, Modifier.size(18.dp))
        Text(texto, modifier = Modifier.padding(start = if (comEstrela) 6.dp else 0.dp))
    }
}

// "Sinopse": indicador enquanto carrega; texto quando chega; nada se não houver ou der erro
@Composable
private fun SecaoSinopse(estado: EstadoSinopse) {
    when (estado) {
        EstadoSinopse.Oculta -> Unit
        EstadoSinopse.Carregando -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Ambar,
            strokeWidth = 2.dp
        )
        is EstadoSinopse.Pronta -> Column(Modifier.padding(bottom = 20.dp)) {
            Text("Sinopse", style = MaterialTheme.typography.headlineSmall)
            Text(
                estado.texto,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Cartão com o controle deslizante (0 a 100), o percentual e o status ao lado
@Composable
private fun Progresso(livro: Livro, aoSalvar: (Int) -> Unit) {
    // Valor que o dedo está arrastando. Só salvamos no Room ao soltar, não a cada pixel.
    // A chave é o progresso salvo: se ele mudar por fora, o controle acompanha.
    var valor by remember(livro.progresso) { mutableStateOf(livro.progresso.toFloat()) }
    val percentual = valor.toInt()
    val status = StatusLeitura.doProgresso(percentual)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Cartao)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = valor,
            onValueChange = { valor = it },
            onValueChangeFinished = { aoSalvar(valor.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Ambar,
                activeTrackColor = Ambar,
                inactiveTrackColor = Color.White
            )
        )
        Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
            Text("$percentual%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextoNoCartao)
            Text(
                when (status) {
                    StatusLeitura.QUERO_LER -> "Quero ler"
                    StatusLeitura.LENDO -> "Lendo"
                    StatusLeitura.LIDO -> "Lido"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextoNoCartao
            )
        }
    }
}

// Mostra a nota e a anotação depois de salvas (nada aparece enquanto não houver avaliação)
@Composable
private fun Avaliacao(livro: Livro) {
    if (livro.notaPessoal == 0 && livro.anotacao.isBlank()) return

    Column(Modifier.padding(top = 20.dp)) {
        Text("Sua avaliação", style = MaterialTheme.typography.headlineSmall)
        if (livro.notaPessoal > 0) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (i < livro.notaPessoal) Ambar else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    "  ${livro.notaPessoal}/5",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (livro.anotacao.isNotBlank()) {
            Text(livro.anotacao, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// Diálogo com 5 estrelas clicáveis (nota de 1 a 5) e um campo de anotação
@Composable
private fun DialogoAvaliacao(
    notaInicial: Int,
    anotacaoInicial: String,
    aoSalvar: (Int, String) -> Unit,
    aoCancelar: () -> Unit
) {
    var nota by remember { mutableStateOf(notaInicial) }
    var anotacao by remember { mutableStateOf(anotacaoInicial) }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Avaliar") },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { i ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "$i ${if (i == 1) "estrela" else "estrelas"}",
                            tint = if (i <= nota) Ambar else Color.Gray,
                            modifier = Modifier.size(44.dp).clickable { nota = i }.padding(4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = anotacao,
                    onValueChange = { anotacao = it },
                    label = { Text("Anotação") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(120.dp)
                )
            }
        },
        // Só deixa salvar com nota de 1 a 5
        confirmButton = { TextButton(onClick = { aoSalvar(nota, anotacao) }, enabled = nota in 1..5) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}
