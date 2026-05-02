package dev.shard9.tonegenerator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.shard9.tonegenerator.audio.ToneGenerator
import dev.shard9.tonegenerator.ui.screens.AboutScreen
import dev.shard9.tonegenerator.ui.screens.GeneratorScreen
import dev.shard9.tonegenerator.ui.screens.ResultsScreen
import dev.shard9.tonegenerator.ui.screens.SettingsScreen
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(toneGenerator: ToneGenerator, viewModel: AppViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerItems = listOf(
        DrawerItem("Generator", "generator", Icons.Default.Menu),
        DrawerItem("Results", "results", Icons.Default.History),
        DrawerItem("Settings", "settings", Icons.Default.Settings),
        DrawerItem("About", "about", Icons.Default.Info)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                if (item.route == "generator") {
                                    popUpTo("generator") { inclusive = true }
                                }
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("LF Tonegen") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "generator",
                modifier = Modifier.padding(padding)
            ) {
                composable("generator") {
                    GeneratorScreen(toneGenerator = toneGenerator, viewModel = viewModel)
                }
                composable("results") {
                    ResultsScreen(viewModel = viewModel)
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
                composable("about") {
                    AboutScreen()
                }
            }
        }
    }
}

data class DrawerItem(val label: String, val route: String, val icon: ImageVector)
