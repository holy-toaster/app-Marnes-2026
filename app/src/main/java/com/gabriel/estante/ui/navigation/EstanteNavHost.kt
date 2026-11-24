package com.gabriel.estante.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gabriel.estante.ui.busca.BuscaScreen
import com.gabriel.estante.ui.estante.EstanteScreen
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
            composable(Destino.ESTANTE.rota) { EstanteScreen() }
            composable(Destino.PESQUISA.rota) { BuscaScreen() }
            composable(Destino.MAIS_AVALIADOS.rota) { TelaTemporaria("Mais Avaliados") }
        }
    }
}

// Tela provisória: só mostra o nome, até ser implementada
@Composable
private fun TelaTemporaria(nome: String) {
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        Text(nome, style = MaterialTheme.typography.headlineLarge)
    }
}
