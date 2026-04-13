package com.daniel.silo.ui

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.daniel.silo.R
import com.daniel.silo.ui.collections.CollectionsScreen
import com.daniel.silo.ui.links.*
import com.daniel.silo.ui.settings.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.daniel.silo.ui.theme.SiloTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SiloTheme {
                SiloNavHost(context = this)
            }
        }
    }
}

sealed class Screen(val route: String, val labelRes: Int) {
    object Links       : Screen("links",       R.string.links)
    object AddLink     : Screen("add_link",    R.string.add_link)
    object Collections : Screen("collections", R.string.collections)
    object Settings    : Screen("settings",    R.string.settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiloNavHost(context: android.content.Context) {
    val navController = rememberNavController()
    val mainVm: MainViewModel = hiltViewModel()
    val uiState by mainVm.uiState.collectAsState()

    val bottomItems = listOf(Screen.Links, Screen.Collections, Screen.Settings)

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        floatingActionButton = {
            if (currentRoute == Screen.Links.route) {
                FloatingActionButton(onClick = { navController.navigate(Screen.AddLink.route) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_link))
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val currentDest = navBackStack?.destination
                bottomItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Links       -> Icon(Icons.Default.Link,       contentDescription = null)
                                Screen.Collections -> Icon(Icons.Default.Folder,     contentDescription = null)
                                Screen.Settings    -> Icon(Icons.Default.Settings,   contentDescription = null)
                                else -> {}
                            }
                        },
                        label = { Text(stringResource(screen.labelRes)) }
                    )
                }
            }
        },
        topBar = {
            TopAppBar(title = { Text("Silo") })
        },
        snackbarHost = {
            uiState.message?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { mainVm.clearMessage() }) { Text("OK") }
                    }
                ) { Text(msg) }
            }
        }
    ) { scaffoldPadding ->
        NavHost(navController, startDestination = Screen.Links.route,
            modifier = Modifier.padding(scaffoldPadding)) {
            composable(Screen.Links.route) {
                LinksScreen(
                    uiState = uiState,
                    onQueryChange = mainVm::setQuery,
                    onSelectCollection = mainVm::selectCollection,
                    onDeleteLink = mainVm::deleteLink,
                    onMoveLink = mainVm::moveLink
                )
            }
            composable(Screen.AddLink.route) {
                val addVm: AddLinkViewModel = hiltViewModel()
                val addState by addVm.state.collectAsState()
                AddLinkScreen(
                    state = addState,
                    onUrlChange = addVm::setUrl,
                    onTitleChange = addVm::setTitle,
                    onDescriptionChange = addVm::setDescription,
                    onCollectionChange = addVm::setCollection,
                    onSave = addVm::save,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Collections.route) {
                CollectionsScreen(
                    collections = uiState.collections,
                    onAdd = mainVm::addCollection,
                    onDelete = mainVm::deleteCollection
                )
            }
            composable(Screen.Settings.route) {
                val settingsVm: SettingsViewModel = hiltViewModel()
                val settings by settingsVm.settings.collectAsState()
                val syncMessage by settingsVm.syncMessage.collectAsState()
                SettingsScreen(
                    settings = settings,
                    onSave = settingsVm::save,
                    onSyncNow = settingsVm::syncNow,
                    syncMessage = syncMessage,
                    onClearSyncMessage = settingsVm::clearSyncMessage
                )
            }
        }
    }
}
