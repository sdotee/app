package s.how.see.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import s.how.see.ui.navigation.SEENavHost
import s.how.see.ui.navigation.TopLevelDestination
import s.how.see.ui.settings.SettingsViewModel
import s.how.see.ui.theme.SEETheme
import s.how.see.ui.theme.ThemeMode

@Composable
fun SEEApp(
    windowSizeClass: WindowSizeClass,
    intent: Intent?,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hasApiKey by settingsViewModel.hasApiKey.collectAsStateWithLifecycle()

    val themeModeStr by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by settingsViewModel.dynamicColor.collectAsStateWithLifecycle()
    val themeMode = when (themeModeStr) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    LaunchedEffect(hasApiKey) {
        if (!hasApiKey) {
            navController.navigate("settings") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = TopLevelDestination.entries.map { it.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    SEETheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == destination.route
                            } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(destination.labelResId)) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            SEENavHost(
                navController = navController,
                onShowSnackbar = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
