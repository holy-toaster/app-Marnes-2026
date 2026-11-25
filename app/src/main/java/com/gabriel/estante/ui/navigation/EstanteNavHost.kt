package com.gabriel.estante.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gabriel.estante.ui.busca.BuscaScreen
import com.gabriel.estante.ui.detalhe.DetalheScreen
import com.gabriel.estante.ui.estante.EstanteScreen
import com.gabriel.estante.ui.ranking.RankingScreen
import com.gabriel.estante.ui.theme.Ambar
import com.gabriel.estante.ui.theme.BarraNavegacao

/** Os 3 destinos da barra inferior: rota, nome (para acessibilidade) e ícone. */
private enum class Destino(val rota: String, val nome: String, val icone: ImageVector) {
    ESTANTE("estante", "Estante", Icons.Filled.Home),
    PESQUISA("pesquisa", "Pesquisa", Icons.Filled.Search),
    MAIS_AVALIADOS("mais_avaliados", "Mais Avaliados", IconeLivro)
}

/** Navegação do app: barra inferior + NavHost com as telas. */
@Composable
fun EstanteNavHost() {
    val navController = rememberNavController()
    val rotaAtual = navController.currentBackStackEntryAsState().value?.destination?.route

    // Abre a tela de Detalhes levando olId, título, autor e capa (vazios se não houver)
    fun abrirDetalhe(olId: String, titulo: String, autor: String, capaUrl: String?) {
        navController.navigate(
            "detalhe/$olId" +
                "?titulo=${Uri.encode(titulo)}" +
                "&autor=${Uri.encode(autor)}" +
                "&capaUrl=${Uri.encode(capaUrl.orEmpty())}"
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = BarraNavegacao) {
                Destino.entries.forEach { destino ->
                    NavigationBarItem(
                        selected = rotaAtual == destino.rota,
                        onClick = {
                            navController.navigate(destino.rota) {
                                // Mantém uma única cópia de cada tela na pilha
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destino.icone, contentDescription = destino.nome) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ambar,
                            unselectedIconColor = Color.White,
                            indicatorColor = BarraNavegacao // sem "bolha" atrás do ícone
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destino.ESTANTE.rota,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destino.ESTANTE.rota) {
                // Livro da estante já está no Room: basta o olId
                EstanteScreen(aoAbrirDetalhe = { olId -> navController.navigate("detalhe/$olId") })
            }
            composable(Destino.PESQUISA.rota) {
                BuscaScreen(aoAbrirDetalhe = { livro ->
                    // Livro da Pesquisa pode não estar no Room: leva também título, autor e capa
                    abrirDetalhe(livro.olId, livro.titulo, livro.autor, livro.capaUrl)
                })
            }
            // FORMA ESCOLHIDA PARA PASSAR DADOS: tudo na própria rota. O olId é obrigatório
            // (parte do caminho) e título, autor e capaUrl são argumentos opcionais de texto
            // (depois do "?"), vazios por padrão. É a forma mais simples: sem objeto
            // compartilhado entre telas e sem ler o banco só para navegar. O Uri.encode acima
            // protege caracteres como "/" e "&" (a URL da capa tem "/" e ":"); a Navigation
            // decodifica sozinha ao ler os argumentos.
            composable(
                route = "detalhe/{olId}?titulo={titulo}&autor={autor}&capaUrl={capaUrl}",
                arguments = listOf(
                    navArgument("olId") { type = NavType.StringType },
                    navArgument("titulo") { type = NavType.StringType; defaultValue = "" },
                    navArgument("autor") { type = NavType.StringType; defaultValue = "" },
                    navArgument("capaUrl") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entrada ->
                val args = entrada.arguments
                DetalheScreen(
                    olId = args?.getString("olId").orEmpty(),
                    titulo = args?.getString("titulo").orEmpty(),
                    autor = args?.getString("autor").orEmpty(),
                    capaUrl = args?.getString("capaUrl")?.ifEmpty { null },
                    aoVoltar = { navController.popBackStack() }
                )
            }
            composable(Destino.MAIS_AVALIADOS.rota) {
                RankingScreen(aoAbrirDetalhe = { l -> abrirDetalhe(l.olId, l.titulo, l.autor, l.capaUrl) })
            }
        }
    }
}
